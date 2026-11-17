package dojo.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hibernate.SessionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Small helpers shared by the contract tests of every stage. */
final class WalletApi {
    private WalletApi() {
    }

    /** An owner no other test uses: the tests share one database. */
    static String unique(String owner) {
        return owner + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static ResultActions postJson(MockMvc mvc, String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    static long open(MockMvc mvc, String owner, String currency) throws Exception {
        String body = postJson(mvc, "/accounts", "{\"owner\": \"" + owner + "\", \"currency\": \"" + currency + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    static ResultActions deposit(MockMvc mvc, long account, long cents) throws Exception {
        return postJson(mvc, "/accounts/" + account + "/deposits", "{\"amountCents\": " + cents + "}");
    }

    static ResultActions withdraw(MockMvc mvc, long account, long cents) throws Exception {
        return postJson(mvc, "/accounts/" + account + "/withdrawals", "{\"amountCents\": " + cents + "}");
    }

    /** An account with that balance already in it. */
    static long funded(MockMvc mvc, String owner, String currency, long cents) throws Exception {
        long id = open(mvc, owner, currency);
        deposit(mvc, id, cents).andExpect(status().isOk());
        return id;
    }

    static long balance(MockMvc mvc, long account) throws Exception {
        String body = mvc.perform(get("/accounts/" + account)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.balanceCents")).longValue();
    }

    static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Runs every call at once on a few threads and returns the HTTP statuses. */
    static List<Integer> together(List<Callable<Integer>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(calls)) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** How many SQL statements Hibernate prepared while `action` ran. */
    static long statementsDuring(EntityManagerFactory factory, ThrowingRunnable action) throws Exception {
        var statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }
}
