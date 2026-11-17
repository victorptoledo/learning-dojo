package dojo.wallet;

import static dojo.wallet.WalletApi.deposit;
import static dojo.wallet.WalletApi.funded;
import static dojo.wallet.WalletApi.open;
import static dojo.wallet.WalletApi.postJson;
import static dojo.wallet.WalletApi.statementsDuring;
import static dojo.wallet.WalletApi.together;
import static dojo.wallet.WalletApi.unique;
import static dojo.wallet.WalletApi.withdraw;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Stage3LedgerTest {
    static final Random RANDOM = new Random();

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /** A made-up currency only this test uses, so the report sees only its accounts. */
    static String currency() {
        return "Q" + (char) ('A' + RANDOM.nextInt(26)) + (char) ('A' + RANDOM.nextInt(26));
    }

    long transfer(long from, long to, long cents) throws Exception {
        String body = postJson(mvc, "/transfers", "{\"from\": " + from + ", \"to\": " + to + ", \"amountCents\": "
                + cents + "}").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void every_change_writes_an_entry_newest_first() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        long bob = open(mvc, unique("bob"), "EUR");
        deposit(mvc, ada, 1000).andExpect(status().isOk());
        withdraw(mvc, ada, 200).andExpect(status().isOk());
        long transfer = transfer(ada, bob, 300);

        mvc.perform(get("/accounts/" + ada + "/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[*].type", contains("TRANSFER_OUT", "WITHDRAWAL", "DEPOSIT")))
                .andExpect(jsonPath("$.content[*].amountCents", contains(300, 200, 1000)))
                .andExpect(jsonPath("$.content[*].balanceAfterCents", contains(500, 800, 1000)))
                .andExpect(jsonPath("$.content[0].transferId").value(transfer))
                .andExpect(jsonPath("$.content[1].transferId").isEmpty())
                .andExpect(jsonPath("$.content[0].at").isString());
        mvc.perform(get("/accounts/" + bob + "/entries"))
                .andExpect(jsonPath("$.content[*].type", contains("TRANSFER_IN")))
                .andExpect(jsonPath("$.content[0].balanceAfterCents").value(300))
                .andExpect(jsonPath("$.content[0].transferId").value(transfer));
    }

    @Test
    void refused_changes_write_no_entry() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 100);
        long bob = open(mvc, unique("bob"), "EUR");
        withdraw(mvc, ada, 500).andExpect(status().isConflict());
        postJson(mvc, "/transfers", "{\"from\": " + ada + ", \"to\": " + bob + ", \"amountCents\": 500}")
                .andExpect(status().isConflict());
        mvc.perform(get("/accounts/" + ada + "/entries")).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/accounts/" + bob + "/entries")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void entries_come_in_pages_and_by_type() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        for (int i = 1; i <= 7; i++) {
            deposit(mvc, ada, i * 10).andExpect(status().isOk());
        }
        withdraw(mvc, ada, 5).andExpect(status().isOk());
        mvc.perform(get("/accounts/" + ada + "/entries").param("page", "1").param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[*].amountCents", contains(50, 40, 30)));
        mvc.perform(get("/accounts/" + ada + "/entries").param("type", "WITHDRAWAL"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amountCents").value(5));
    }

    @Test
    void bad_page_parameters_are_400() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        for (String[] params : List.of(new String[] {"size", "0"}, new String[] {"size", "101"},
                new String[] {"page", "-1"}, new String[] {"type", "GIFT"})) {
            mvc.perform(get("/accounts/" + ada + "/entries").param(params[0], params[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mvc.perform(get("/accounts/987654/entries")).andExpect(status().isNotFound());
    }

    @Test
    void a_page_of_entries_runs_at_most_three_statements() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        for (int i = 0; i < 6; i++) {
            deposit(mvc, ada, 10).andExpect(status().isOk());
        }
        long statements = statementsDuring(entityManagerFactory, () ->
                mvc.perform(get("/accounts/" + ada + "/entries").param("size", "4")).andExpect(status().isOk()));
        assertTrue(statements <= 3, "a page of entries ran " + statements + " SQL statements");
    }

    @Test
    void the_report_lists_the_richest_open_accounts_with_their_entries() throws Exception {
        String currency = currency();
        String ada = unique("ada");
        String bob = unique("bob");
        String cy = unique("cy");
        long adaId = funded(mvc, ada, currency, 500);
        deposit(mvc, adaId, 500).andExpect(status().isOk());
        funded(mvc, bob, currency, 2000);
        funded(mvc, cy, currency, 1000);
        long gone = open(mvc, unique("gone"), currency);
        mvc.perform(post("/accounts/" + gone + "/close")).andExpect(status().isOk());
        open(mvc, unique("empty"), currency);

        mvc.perform(get("/reports/balances").param("currency", currency).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].balanceCents", contains(2000, 1000, 1000)))
                .andExpect(jsonPath("$[0].owner").value(bob))
                .andExpect(jsonPath("$[*].owner", contains(bob, ada, cy)))
                .andExpect(jsonPath("$[*].entries", contains(1, 2, 1)));
        mvc.perform(get("/reports/balances").param("currency", currency))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[3].balanceCents").value(0))
                .andExpect(jsonPath("$[3].entries").value(0));
        mvc.perform(get("/reports/balances").param("currency", currency).param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void the_report_runs_one_statement() throws Exception {
        String currency = currency();
        for (int i = 0; i < 6; i++) {
            long id = funded(mvc, unique("many"), currency, 100 + i);
            deposit(mvc, id, 1).andExpect(status().isOk());
        }
        long statements = statementsDuring(entityManagerFactory, () ->
                mvc.perform(get("/reports/balances").param("currency", currency)).andExpect(status().isOk())
                        .andExpect(jsonPath("$", hasSize(6))));
        assertEquals(1, statements, "the report ran " + statements + " SQL statements (N+1?)");
    }

    @Test
    void entries_written_under_load_add_up() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(() -> deposit(mvc, ada, 100).andReturn().getResponse().getStatus());
        }
        together(calls);
        String body = mvc.perform(get("/accounts/" + ada + "/entries").param("size", "100"))
                .andReturn().getResponse().getContentAsString();
        List<Number> after = JsonPath.read(body, "$.content[*].balanceAfterCents");
        List<Long> sorted = after.stream().map(Number::longValue).sorted().toList();
        assertEquals(LongStream.rangeClosed(1, 20).map(i -> i * 100).boxed().toList(), sorted);
    }
}
