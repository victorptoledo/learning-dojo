package dojo.wallet;

import static dojo.wallet.WalletApi.balance;
import static dojo.wallet.WalletApi.deposit;
import static dojo.wallet.WalletApi.funded;
import static dojo.wallet.WalletApi.open;
import static dojo.wallet.WalletApi.postJson;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class Stage2TransfersTest {
    @Autowired
    MockMvc mvc;

    ResultActions transfer(long from, long to, long cents) throws Exception {
        return postJson(mvc, "/transfers", "{\"from\": " + from + ", \"to\": " + to + ", \"amountCents\": " + cents + "}");
    }

    @Test
    void a_transfer_moves_money_and_has_a_location() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = open(mvc, unique("bob"), "EUR");
        String location = transfer(ada, bob, 300)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.from").value(ada))
                .andExpect(jsonPath("$.to").value(bob))
                .andExpect(jsonPath("$.amountCents").value(300))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/transfers/\\d+"), "Location was " + location);
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.amountCents").value(300));
        assertEquals(700, balance(mvc, ada));
        assertEquals(300, balance(mvc, bob));
        mvc.perform(get("/transfers/987654")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("transfer 987654 not found"));
    }

    @Test
    void a_bad_transfer_is_400() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        postJson(mvc, "/transfers", "{\"amountCents\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.from").isString())
                .andExpect(jsonPath("$.errors.to").isString())
                .andExpect(jsonPath("$.errors.amountCents").isString());
        transfer(ada, ada, 100)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.to").isString());
        assertEquals(1000, balance(mvc, ada));
    }

    @Test
    void refused_transfers_change_nothing() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 500);
        long bob = funded(mvc, unique("bob"), "EUR", 100);
        long dollars = funded(mvc, unique("cy"), "USD", 100);

        transfer(ada, 987654, 100).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("account 987654 not found"));
        transfer(987654, ada, 100).andExpect(status().isNotFound());
        transfer(ada, dollars, 100).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("cannot move EUR to USD"));
        transfer(ada, bob, 501).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("account " + ada + " has 500 cents, needs 501"));

        assertEquals(500, balance(mvc, ada));
        assertEquals(100, balance(mvc, bob));
        assertEquals(100, balance(mvc, dollars));
    }

    @Test
    void closed_accounts_take_and_give_nothing() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 500);
        long gone = funded(mvc, unique("gone"), "EUR", 200);
        mvc.perform(post("/accounts/" + gone + "/close")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("account " + gone + " still has 200 cents"));
        withdraw(mvc, gone, 200).andExpect(status().isOk());
        mvc.perform(post("/accounts/" + gone + "/close")).andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
        mvc.perform(post("/accounts/" + gone + "/close")).andExpect(status().isConflict());
        mvc.perform(get("/accounts/" + ada)).andExpect(jsonPath("$.closed").value(false));

        String closed = "account " + gone + " is closed";
        transfer(ada, gone, 100).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(closed));
        transfer(gone, ada, 100).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(closed));
        deposit(mvc, gone, 100).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(closed));
        withdraw(mvc, gone, 100).andExpect(status().isConflict());
        assertEquals(500, balance(mvc, ada));
        assertEquals(0, balance(mvc, gone));
    }

    @Test
    void concurrent_transfers_never_overdraw() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = open(mvc, unique("bob"), "EUR");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(() -> transfer(ada, bob, 100).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(10, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
        assertEquals(10, statuses.stream().filter(s -> s == 409).count(), "statuses: " + statuses);
        assertEquals(0, balance(mvc, ada));
        assertEquals(1000, balance(mvc, bob));
    }

    @Test
    void opposite_transfers_at_once_all_pass_and_keep_the_total() throws Exception {
        long ada = funded(mvc, unique("ada"), "EUR", 1000);
        long bob = funded(mvc, unique("bob"), "EUR", 1000);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            calls.add(() -> transfer(ada, bob, 50).andReturn().getResponse().getStatus());
            calls.add(() -> transfer(bob, ada, 30).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(24, statuses.stream().filter(s -> s == 201).count(), "statuses: " + statuses);
        assertEquals(1000 - 12 * 50 + 12 * 30, balance(mvc, ada));
        assertEquals(1000 + 12 * 50 - 12 * 30, balance(mvc, bob));
    }

    @Test
    void concurrent_deposits_all_count() throws Exception {
        long ada = open(mvc, unique("ada"), "EUR");
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            calls.add(() -> deposit(mvc, ada, 100).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = together(calls);
        assertEquals(20, statuses.stream().filter(s -> s == 200).count(), "statuses: " + statuses);
        assertEquals(2000, balance(mvc, ada));
    }
}
