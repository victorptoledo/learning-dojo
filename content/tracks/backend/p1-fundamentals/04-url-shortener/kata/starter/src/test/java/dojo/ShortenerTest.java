package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashSet;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ShortenerTest {
    @Autowired
    MockMvc mvc;

    ResultActions shorten(String json) throws Exception {
        return mvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String code(String json) throws Exception {
        return JsonPath.read(shorten(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.code");
    }

    @Test
    void a_short_link_redirects() throws Exception {
        String code = code("{\"url\": \"https://example.com\"}");
        mvc.perform(get("/" + code)).andExpect(status().isFound());
    }
}
