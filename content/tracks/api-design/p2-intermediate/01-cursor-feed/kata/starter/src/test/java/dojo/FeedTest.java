package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FeedTest {
    @Autowired
    MockMvc mvc;

    @Test
    void the_first_page_has_twenty_events_and_a_next_cursor() throws Exception {
        mvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(20))
                .andExpect(jsonPath("$.nextCursor").isString());
    }

    @Test
    void a_garbage_cursor_is_a_400() throws Exception {
        mvc.perform(get("/events").param("cursor", "not a cursor"))
                .andExpect(status().isBadRequest());
    }
}
