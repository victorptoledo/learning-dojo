package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GreetingGradingTest {
    @Autowired
    MockMvc mvc;

    private void expect(String body, String name) throws Exception {
        var request = get("/greet");
        if (name != null) {
            request = request.param("name", name);
        }
        mvc.perform(request).andExpect(status().isOk()).andExpect(content().string(body));
    }

    @Test
    void greets_another_name() throws Exception {
        expect("Welcome to the dojo, Duke!", "Duke");
    }

    @Test
    void ignores_surrounding_spaces() throws Exception {
        expect("Welcome to the dojo, Ada!", "  Ada ");
    }

    @Test
    void greets_a_stranger_without_a_name() throws Exception {
        expect("Welcome to the dojo, stranger!", null);
    }

    @Test
    void greets_a_stranger_when_the_name_is_blank() throws Exception {
        expect("Welcome to the dojo, stranger!", "   ");
        expect("Welcome to the dojo, stranger!", "");
    }

    @Test
    void other_paths_are_not_found() throws Exception {
        mvc.perform(get("/greet").param("name", "Ada")).andExpect(status().isOk());
        mvc.perform(get("/hello")).andExpect(status().isNotFound());
    }
}
