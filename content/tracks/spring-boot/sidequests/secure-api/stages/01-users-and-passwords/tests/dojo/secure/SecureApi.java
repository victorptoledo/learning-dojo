package dojo.secure;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Small helpers shared by the contract tests of every stage. */
final class SecureApi {
    static final String PASSWORD = "correct-horse-battery";

    private SecureApi() {
    }

    /** A username no other test uses: the tests share one database. */
    static String unique(String name) {
        return name + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    static ResultActions signup(MockMvc mvc, String username, String password) throws Exception {
        return mvc.perform(post("/api/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"));
    }

    /** Signs a new user up and returns the HTTP Basic login for it. */
    static RequestPostProcessor newUser(MockMvc mvc, String name) throws Exception {
        String username = unique(name);
        signup(mvc, username, PASSWORD).andExpect(status().isCreated());
        return httpBasic(username, PASSWORD);
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
}
