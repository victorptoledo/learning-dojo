package dojo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InvoicesTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_clerk_issues_an_invoice() throws Exception {
        mvc.perform(post("/invoices").with(httpBasic("clerk", "clerk-pass")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"Umbrella\", \"amountCents\": 4200}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(org.hamcrest.Matchers.startsWith("INV-")));
    }

    @Test
    void the_readiness_probe_is_public() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void the_environment_is_not_exposed() throws Exception {
        mvc.perform(get("/actuator/env").with(httpBasic("admin", "admin-pass"))).andExpect(status().isNotFound());
    }
}
