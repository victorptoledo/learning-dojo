package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dojo.Partners.Credentials;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** A few of the rules, to start from. They fail until the API follows the README. */
@SpringBootTest
@AutoConfigureMockMvc
class ExportsTest {
    @Autowired MockMvc mvc;
    @Autowired Partners partners;
    @Autowired BackgroundJobs jobs;

    @Test
    void an_export_runs_in_the_background() throws Exception {
        Credentials acme = partners.register("acme-test", "exports:read", "exports:write");
        mvc.perform(post("/exports")
                        .header("Authorization", "Bearer " + acme.apiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataset\": \"orders\", \"format\": \"csv\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
        jobs.runDue();
    }

    @Test
    void no_key_is_401() throws Exception {
        mvc.perform(get("/exports/exp_1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }
}
