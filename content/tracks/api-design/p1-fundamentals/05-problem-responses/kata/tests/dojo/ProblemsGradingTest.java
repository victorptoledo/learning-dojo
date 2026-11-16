package dojo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ProblemsGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired
    MockMvc mvc;

    ResultActions withdraw(String account, String json) throws Exception {
        return mvc.perform(post("/accounts/" + account + "/withdrawals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    static ResultActions isProblem(ResultActions actions, int status) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isString());
    }

    @Test
    void a_withdrawal_is_201_created_with_its_location() throws Exception {
        withdraw("acc-1", """
                {"amountCents": 1500, "reference": "groceries"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/accounts/acc-1/withdrawals/")))
                .andExpect(jsonPath("$.accountId").value("acc-1"))
                .andExpect(jsonPath("$.amountCents").value(1500))
                .andExpect(jsonPath("$.reference").value("groceries"));
    }

    @Test
    void an_unknown_account_is_a_404_problem_with_type_title_detail_and_instance() throws Exception {
        isProblem(mvc.perform(get("/accounts/acc-9")), 404)
                .andExpect(jsonPath("$.type").value(TYPES + "account-not-found"))
                .andExpect(jsonPath("$.title").value("Account not found"))
                .andExpect(jsonPath("$.detail").value("account acc-9 does not exist"))
                .andExpect(jsonPath("$.instance").value("/accounts/acc-9"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void withdrawing_from_an_unknown_account_is_404_too() throws Exception {
        isProblem(withdraw("acc-9", """
                {"amountCents": 100, "reference": "rent"}
                """), 404)
                .andExpect(jsonPath("$.type").value(TYPES + "account-not-found"))
                .andExpect(jsonPath("$.instance").value("/accounts/acc-9/withdrawals"));
    }

    @Test
    void a_frozen_account_is_a_409_conflict_problem() throws Exception {
        isProblem(withdraw("acc-3", """
                {"amountCents": 100, "reference": "rent"}
                """), 409)
                .andExpect(jsonPath("$.type").value(TYPES + "account-frozen"))
                .andExpect(jsonPath("$.title").value("Account frozen"))
                .andExpect(jsonPath("$.detail").value("account acc-3 is frozen"));
    }

    @Test
    void insufficient_funds_is_a_422_problem_with_extension_members() throws Exception {
        isProblem(withdraw("acc-2", """
                {"amountCents": 900, "reference": "bike"}
                """), 422)
                .andExpect(jsonPath("$.type").value(TYPES + "insufficient-funds"))
                .andExpect(jsonPath("$.title").value("Insufficient funds"))
                .andExpect(jsonPath("$.detail").value("balance 500 is less than the 900 requested"))
                .andExpect(jsonPath("$.balance").value(500))
                .andExpect(jsonPath("$.requested").value(900));
        mvc.perform(get("/accounts/acc-2")).andExpect(jsonPath("$.balanceCents").value(500));
    }

    @Test
    void invalid_fields_are_a_400_problem_with_one_json_pointer_per_error() throws Exception {
        isProblem(withdraw("acc-1", """
                {"amountCents": -5, "reference": ""}
                """), 400)
                .andExpect(jsonPath("$.type").value(TYPES + "validation-error"))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("the request has 2 invalid fields"))
                .andExpect(jsonPath("$.instance").value("/accounts/acc-1/withdrawals"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/amountCents"))
                .andExpect(jsonPath("$.errors[0].detail").isString())
                .andExpect(jsonPath("$.errors[1].pointer").value("#/reference"));
    }

    @Test
    void one_invalid_field_says_so_in_the_singular() throws Exception {
        isProblem(withdraw("acc-1", """
                {"reference": "no amount"}
                """), 400)
                .andExpect(jsonPath("$.detail").value("the request has 1 invalid field"))
                .andExpect(jsonPath("$.errors[0].pointer").value("#/amountCents"));
    }

    @Test
    void malformed_json_is_a_400_problem() throws Exception {
        isProblem(withdraw("acc-1", "{\"amountCents\": "), 400);
    }

    @Test
    void an_unknown_path_is_a_404_problem() throws Exception {
        isProblem(mvc.perform(get("/no/such/thing")), 404);
    }

    @Test
    void a_bug_is_a_500_problem_that_leaks_nothing() throws Exception {
        String body = isProblem(withdraw("acc-1", """
                {"amountCents": 666, "reference": "the bug"}
                """), 500)
                .andExpect(jsonPath("$.detail").value("an unexpected error occurred"))
                .andExpect(jsonPath("$.instance", endsWith("/withdrawals")))
                .andReturn().getResponse().getContentAsString();
        org.hamcrest.MatcherAssert.assertThat(body, not(containsString("10.0.3.7")));
        org.hamcrest.MatcherAssert.assertThat(body, not(containsString("Ledger")));
        org.hamcrest.MatcherAssert.assertThat(body, not(containsString("Exception")));
    }
}
