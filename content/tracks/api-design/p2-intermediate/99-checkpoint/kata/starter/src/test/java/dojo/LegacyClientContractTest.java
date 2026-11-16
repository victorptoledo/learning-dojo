package dojo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * What the mobile app (still on the first version of the API) relies on. It is a consumer
 * contract: every expectation here must keep passing, whatever the server becomes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LegacyClientContractTest {
    @Autowired
    MockMvc mvc;

    @Test
    void reads_a_customer_with_name_email_phone_and_status() throws Exception {
        mvc.perform(get("/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.phone").value("+44 20 7946 0001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void the_email_is_the_primary_one() throws Exception {
        mvc.perform(get("/customers/2")).andExpect(jsonPath("$.email").value("linus@example.org"));
    }

    @Test
    void the_status_is_one_of_the_two_values_the_app_knows() throws Exception {
        mvc.perform(get("/customers/3")).andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void signs_up_a_customer_with_name_email_and_phone() throws Exception {
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Alan Turing", "email": "alan@example.com", "phone": "+44 161 000 0000"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/customers/")))
                .andExpect(jsonPath("$.name").value("Alan Turing"))
                .andExpect(jsonPath("$.email").value("alan@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void a_missing_customer_is_404() throws Exception {
        mvc.perform(get("/customers/424242")).andExpect(status().isNotFound());
    }
}
