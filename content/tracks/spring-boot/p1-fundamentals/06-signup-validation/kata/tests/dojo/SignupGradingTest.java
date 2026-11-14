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

import static org.hamcrest.Matchers.endsWith;

@SpringBootTest
@AutoConfigureMockMvc
class SignupGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions signup(String json) throws Exception {
        return mvc.perform(post("/signups").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void a_valid_signup_is_created_with_a_normalised_email() throws Exception {
        String json = signup("""
                {"name": " Ada ", "email": "ADA@Example.com", "age": 36}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/members/")))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(json, "$.id");
        mvc.perform(get("/members/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.age").value(36));
    }

    @Test
    void invalid_fields_are_a_400_problem_listing_each_field() throws Exception {
        signup("""
                {"name": "", "email": "not-an-email", "age": 10}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    void missing_fields_are_rejected_too() throws Exception {
        signup("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    void a_name_longer_than_40_characters_is_rejected() throws Exception {
        signup("{\"name\": \"" + "x".repeat(41) + "\", \"email\": \"long@example.com\", \"age\": 20}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").doesNotExist());
    }

    @Test
    void the_same_email_twice_is_a_409_conflict() throws Exception {
        signup("""
                {"name": "Linus", "email": "linus@example.com", "age": 30}
                """).andExpect(status().isCreated());
        signup("""
                {"name": "Other", "email": "LINUS@Example.com", "age": 31}
                """)
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("email linus@example.com is already registered"));
    }

    @Test
    void an_unknown_member_is_a_404_problem() throws Exception {
        mvc.perform(get("/members/424242"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("member 424242 not found"));
    }

    @Test
    void malformed_json_is_a_400_problem_too() throws Exception {
        signup("{\"name\": ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
