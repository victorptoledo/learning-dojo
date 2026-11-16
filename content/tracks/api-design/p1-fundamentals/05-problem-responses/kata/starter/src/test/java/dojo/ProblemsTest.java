package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ProblemsTest {
    @Autowired
    MockMvc mvc;

    @Test
    void an_existing_account_is_200() throws Exception {
        mvc.perform(get("/accounts/acc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("Ada"));
    }

    @Test
    void an_unknown_account_is_a_404_problem() throws Exception {
        mvc.perform(get("/accounts/acc-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.dojo.example/problems/account-not-found"));
    }
}
