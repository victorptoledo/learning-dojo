package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class SignupTest {
    @Autowired
    MockMvc mvc;

    ResultActions signup(String json) throws Exception {
        return mvc.perform(post("/signups").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void a_valid_signup_is_created() throws Exception {
        signup("""
                {"name": "Grace", "email": "grace@example.com", "age": 40}
                """).andExpect(status().isCreated());
    }

    @Test
    void a_blank_name_is_rejected() throws Exception {
        signup("""
                {"name": " ", "email": "blank@example.com", "age": 40}
                """).andExpect(status().isBadRequest());
    }
}
