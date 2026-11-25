package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CheckoutTest {
    static final String BOOK = """
            {"customerName": "Ada Lovelace", "sku": "BOOK", "quantity": 1, "totalCents": 2500, "address": "1 Main St"}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    FlagStore flags;

    @Test
    void with_the_flag_off_the_monolith_serves_the_checkout() throws Exception {
        mvc.perform(post("/checkout").header("X-User", "ada").contentType(MediaType.APPLICATION_JSON).content(BOOK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.path").value("legacy"));
    }

    @Test
    void with_the_flag_on_the_saga_serves_the_checkout() throws Exception {
        flags.set(FlagStore.SAGA_CHECKOUT, 100, Set.of());
        mvc.perform(post("/checkout").header("X-User", "ada").contentType(MediaType.APPLICATION_JSON).content(BOOK))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.path").value("saga"));
    }
}
