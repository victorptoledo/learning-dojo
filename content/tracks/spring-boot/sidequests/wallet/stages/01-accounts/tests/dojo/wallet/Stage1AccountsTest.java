package dojo.wallet;

import static dojo.wallet.WalletApi.balance;
import static dojo.wallet.WalletApi.deposit;
import static dojo.wallet.WalletApi.funded;
import static dojo.wallet.WalletApi.open;
import static dojo.wallet.WalletApi.postJson;
import static dojo.wallet.WalletApi.together;
import static dojo.wallet.WalletApi.unique;
import static dojo.wallet.WalletApi.withdraw;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Stage1AccountsTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_new_account_starts_empty() throws Exception {
        String owner = unique("ada");
        String location = postJson(mvc, "/accounts", "{\"owner\": \"  " + owner + " \", \"currency\": \"EUR\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.owner").value(owner))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balanceCents").value(0))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/accounts/\\d+"), "Location was " + location);
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.owner").value(owner));
    }

    @Test
    void a_bad_account_is_400_with_the_bad_fields() throws Exception {
        postJson(mvc, "/accounts", "{\"owner\": \" \", \"currency\": \"eur\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.owner").isString())
                .andExpect(jsonPath("$.errors.currency").isString());
        postJson(mvc, "/accounts", "{\"owner\": \"" + unique("x") + "\", \"currency\": \"EURO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").isString())
                .andExpect(jsonPath("$.errors.owner").doesNotExist());
    }

    @Test
    void one_account_per_owner_and_currency() throws Exception {
        String owner = unique("grace");
        open(mvc, owner, "EUR");
        open(mvc, owner, "USD");
        postJson(mvc, "/accounts", "{\"owner\": \"" + owner + "\", \"currency\": \"EUR\"}")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/accounts").param("owner", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].currency", contains("EUR", "USD")));
    }

    @Test
    void the_database_refuses_a_duplicate_even_when_requests_race() throws Exception {
        String owner = unique("racer");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            calls.add(() -> postJson(mvc, "/accounts", "{\"owner\": \"" + owner + "\", \"currency\": \"GBP\"}")
                    .andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
        assertEquals(11, statuses.stream().filter(s -> s == 409).count(), "statuses: " + statuses);
    }

    @Test
    void deposits_and_withdrawals_change_the_balance() throws Exception {
        long id = open(mvc, unique("linus"), "EUR");
        deposit(mvc, id, 1500).andExpect(status().isOk()).andExpect(jsonPath("$.balanceCents").value(1500));
        withdraw(mvc, id, 400).andExpect(status().isOk()).andExpect(jsonPath("$.balanceCents").value(1100));
        assertEquals(1100, balance(mvc, id));
    }

    @Test
    void a_withdrawal_never_goes_below_zero() throws Exception {
        long id = funded(mvc, unique("margaret"), "EUR", 500);
        withdraw(mvc, id, 800)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("account " + id + " has 500 cents, needs 800"));
        assertEquals(500, balance(mvc, id));
        withdraw(mvc, id, 500).andExpect(jsonPath("$.balanceCents").value(0));
    }

    @Test
    void amounts_are_checked() throws Exception {
        long id = open(mvc, unique("barbara"), "EUR");
        deposit(mvc, id, 0).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.amountCents").isString());
        deposit(mvc, id, -5).andExpect(status().isBadRequest());
        deposit(mvc, id, 100000001).andExpect(status().isBadRequest());
        withdraw(mvc, id, 0).andExpect(status().isBadRequest());
        assertEquals(0, balance(mvc, id));
    }

    @Test
    void an_unknown_account_is_404() throws Exception {
        mvc.perform(get("/accounts/987654"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("account 987654 not found"));
        deposit(mvc, 987654, 100).andExpect(status().isNotFound());
        withdraw(mvc, 987654, 100).andExpect(status().isNotFound());
    }
}
