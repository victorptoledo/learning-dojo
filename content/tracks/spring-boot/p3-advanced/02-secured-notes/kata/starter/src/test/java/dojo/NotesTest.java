package dojo;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class NotesTest {
    @Autowired
    MockMvc mvc;

    static final RequestPostProcessor ADA = httpBasic("ada", "ada-pass");
    static final RequestPostProcessor LINUS = httpBasic("linus", "linus-pass");
    static final RequestPostProcessor ROOT = httpBasic("root", "root-pass");

    long write(RequestPostProcessor user, String text) throws Exception {
        String json = mvc.perform(post("/notes").with(user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void a_user_reads_back_a_note() throws Exception {
        long id = write(ADA, "hello");
        mvc.perform(get("/notes/" + id).with(ADA)).andExpect(status().isOk());
    }

    @Test
    void anonymous_requests_are_401() throws Exception {
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized());
    }
}
