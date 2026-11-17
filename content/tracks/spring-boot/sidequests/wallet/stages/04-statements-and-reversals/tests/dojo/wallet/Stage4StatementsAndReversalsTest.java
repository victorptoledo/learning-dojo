package dojo.wallet;

import static dojo.wallet.WalletApi.balance;
import static dojo.wallet.WalletApi.deposit;
import static dojo.wallet.WalletApi.funded;
import static dojo.wallet.WalletApi.idOf;
import static dojo.wallet.WalletApi.open;
import static dojo.wallet.WalletApi.postJson;
import static dojo.wallet.WalletApi.statementsDuring;
import static dojo.wallet.WalletApi.together;
import static dojo.wallet.WalletApi.unique;
import static dojo.wallet.WalletApi.withdraw;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class Stage4StatementsAndReversalsTest {

    @TestConfiguration
    static class Clocks {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    long transfer(long from, long to, long cents) throws Exception {
        return idOf(postJson(mvc, "/transfers", "{\"from\": " + from + ", \"to\": " + to + ", \"amountCents\": "
                + cents + "}").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    ResultActions statement(long account, String month) throws Exception {
        return mvc.perform(get("/accounts/" + account + "/statement").param("month", month));
    }

    ResultActions reverse(long transfer) throws Exception {
        return mvc.perform(post("/transfers/" + transfer + "/reversal"));
    }

    @Test
    void a_statement_sums_up_one_month() throws Exception {
        clock.set("2099-10-20T10:00:00Z");
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = open(mvc, unique("bob"), "EUR");
        clock.set("2099-11-01T00:00:00Z");
        withdraw(mvc, ada, 200).andExpect(status().isOk());
        clock.set("2099-11-15T12:00:00Z");
        transfer(ada, bob, 300);
        clock.set("2099-11-30T23:59:59Z");
        deposit(mvc, ada, 50).andExpect(status().isOk());
        clock.set("2099-12-01T00:00:00Z");
        deposit(mvc, ada, 7).andExpect(status().isOk());

        statement(ada, "2099-11")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2099-11"))
                .andExpect(jsonPath("$.openingBalanceCents").value(1000))
                .andExpect(jsonPath("$.closingBalanceCents").value(550))
                .andExpect(jsonPath("$.inCents").value(50))
                .andExpect(jsonPath("$.outCents").value(500))
                .andExpect(jsonPath("$.entries").value(3));
        statement(ada, "2099-10")
                .andExpect(jsonPath("$.openingBalanceCents").value(0))
                .andExpect(jsonPath("$.closingBalanceCents").value(1000))
                .andExpect(jsonPath("$.inCents").value(1000))
                .andExpect(jsonPath("$.outCents").value(0))
                .andExpect(jsonPath("$.entries").value(1));
        statement(ada, "2099-09")
                .andExpect(jsonPath("$.openingBalanceCents").value(0))
                .andExpect(jsonPath("$.closingBalanceCents").value(0))
                .andExpect(jsonPath("$.entries").value(0));
        statement(ada, "2100-01")
                .andExpect(jsonPath("$.openingBalanceCents").value(557))
                .andExpect(jsonPath("$.closingBalanceCents").value(557))
                .andExpect(jsonPath("$.entries").value(0));
        statement(bob, "2099-11")
                .andExpect(jsonPath("$.inCents").value(300))
                .andExpect(jsonPath("$.closingBalanceCents").value(300));
    }

    @Test
    void a_statement_needs_a_month_and_an_account() throws Exception {
        clock.set("2099-11-05T08:00:00Z");
        long ada = open(mvc, unique("ada"), "EUR");
        for (String month : List.of("2099-13", "november")) {
            statement(ada, month)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        mvc.perform(get("/accounts/" + ada + "/statement")).andExpect(status().isBadRequest());
        statement(987654, "2099-11").andExpect(status().isNotFound());
    }

    @Test
    void a_statement_runs_at_most_four_statements() throws Exception {
        clock.set("2099-10-10T08:00:00Z");
        long ada = funded(mvc, unique("ada"), "EUR", 100);
        clock.set("2099-11-10T08:00:00Z");
        for (int i = 0; i < 8; i++) {
            deposit(mvc, ada, 10).andExpect(status().isOk());
        }
        long statements = statementsDuring(entityManagerFactory, () ->
                statement(ada, "2099-11").andExpect(status().isOk()).andExpect(jsonPath("$.entries").value(8)));
        assertTrue(statements <= 4, "the statement ran " + statements + " SQL statements");
    }

    @Test
    void a_reversal_undoes_a_transfer_once() throws Exception {
        clock.set("2099-11-20T09:00:00Z");
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = open(mvc, unique("bob"), "EUR");
        long original = transfer(ada, bob, 300);
        mvc.perform(get("/transfers/" + original)).andExpect(jsonPath("$.reverses").isEmpty());

        String location = reverse(original)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.from").value(bob))
                .andExpect(jsonPath("$.to").value(ada))
                .andExpect(jsonPath("$.amountCents").value(300))
                .andExpect(jsonPath("$.reverses").value(original))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/transfers/\\d+"), "Location was " + location);
        assertEquals(1000, balance(mvc, ada));
        assertEquals(0, balance(mvc, bob));

        reverse(original).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("transfer " + original + " is already reversed"));
        long reversal = idOf(mvc.perform(get(location)).andReturn().getResponse().getContentAsString());
        reverse(reversal).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("a reversal cannot be reversed"));
        reverse(987654).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("transfer 987654 not found"));
        assertEquals(1000, balance(mvc, ada));
    }

    @Test
    void a_reversal_needs_the_money_back() throws Exception {
        clock.set("2099-11-21T09:00:00Z");
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = open(mvc, unique("bob"), "EUR");
        long original = transfer(ada, bob, 300);
        withdraw(mvc, bob, 250).andExpect(status().isOk());

        reverse(original).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("account " + bob + " has 50 cents, needs 300"));
        assertEquals(700, balance(mvc, ada));
        assertEquals(50, balance(mvc, bob));

        deposit(mvc, bob, 250).andExpect(status().isOk());
        reverse(original).andExpect(status().isCreated());
        assertEquals(1000, balance(mvc, ada));
        assertEquals(0, balance(mvc, bob));
    }

    @Test
    void ten_reversals_at_once_undo_it_once() throws Exception {
        clock.set("2099-11-22T09:00:00Z");
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = funded(mvc, unique("bob"), "EUR", 5000);
        long original = transfer(ada, bob, 400);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            calls.add(() -> reverse(original).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(1, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
        assertEquals(9, statuses.stream().filter(s -> s == 409).count(), "statuses: " + statuses);
        assertEquals(1000, balance(mvc, ada));
        assertEquals(5000, balance(mvc, bob));
    }
}
