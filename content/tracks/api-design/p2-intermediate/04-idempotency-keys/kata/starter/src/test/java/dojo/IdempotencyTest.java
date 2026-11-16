package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_payment_with_a_key_is_created() throws Exception {
        mvc.perform(post("/payments").header("X-Api-Key", "shop-1").header("Idempotency-Key", "visible-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\": \"acct-1\", \"amountCents\": 1000}"))
                .andExpect(status().isCreated());
    }

    @Test
    void a_payment_without_a_key_is_refused() throws Exception {
        mvc.perform(post("/payments").header("X-Api-Key", "shop-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\": \"acct-1\", \"amountCents\": 1000}"))
                .andExpect(status().isBadRequest());
    }
}
