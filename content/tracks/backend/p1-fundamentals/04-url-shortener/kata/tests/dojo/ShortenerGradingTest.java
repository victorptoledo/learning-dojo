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
class ShortenerGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions shorten(String json) throws Exception {
        return mvc.perform(post("/links").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String code(String json) throws Exception {
        return JsonPath.read(shorten(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.code");
    }

    @Test
    void a_custom_code_redirects_with_302_to_the_target() throws Exception {
        shorten("{\"url\": \"https://example.com/docs?page=2\", \"code\": \"docs-2\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/links/docs-2")))
                .andExpect(jsonPath("$.clicks").value(0));
        mvc.perform(get("/docs-2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs?page=2"));
    }

    @Test
    void generated_codes_are_seven_letters_or_digits_and_unique() throws Exception {
        var codes = new HashSet<String>();
        for (int i = 0; i < 40; i++) {
            String code = code("{\"url\": \"https://example.com/" + i + "\"}");
            assertTrue(code.matches("[A-Za-z0-9]{7}"), code);
            codes.add(code);
        }
        assertEquals(40, codes.size());
    }

    @Test
    void only_absolute_http_and_https_urls_are_accepted() throws Exception {
        shorten("{\"url\": \"ftp://example.com/file\"}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"/relative/path\"}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"javascript:alert(1)\"}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"not a url\"}").andExpect(status().isBadRequest());
        shorten("{}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"http://example.com\"}").andExpect(status().isCreated());
    }

    @Test
    void custom_codes_must_be_valid_and_free() throws Exception {
        shorten("{\"url\": \"https://example.com\", \"code\": \"ab\"}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"https://example.com\", \"code\": \"has space\"}").andExpect(status().isBadRequest());
        shorten("{\"url\": \"https://example.com\", \"code\": \"taken-code\"}").andExpect(status().isCreated());
        shorten("{\"url\": \"https://other.com\", \"code\": \"taken-code\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("code taken-code is taken"));
    }

    @Test
    void unknown_codes_are_404() throws Exception {
        mvc.perform(get("/nope-nope")).andExpect(status().isNotFound());
        mvc.perform(get("/links/nope-nope")).andExpect(status().isNotFound());
    }

    @Test
    void every_redirect_counts_one_click_even_concurrently() throws Exception {
        shorten("{\"url\": \"https://example.com/hot\", \"code\": \"hot-link\"}");
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 40; i++) {
                executor.submit(() -> {
                    mvc.perform(get("/hot-link")).andExpect(status().isFound());
                    return null;
                });
            }
        }
        mvc.perform(get("/links/hot-link")).andExpect(jsonPath("$.clicks").value(40));
    }

    @Test
    void top_lists_the_most_clicked_first() throws Exception {
        shorten("{\"url\": \"https://example.com/a\", \"code\": \"top-a\"}");
        shorten("{\"url\": \"https://example.com/b\", \"code\": \"top-b\"}");
        for (int i = 0; i < 100; i++) {
            mvc.perform(get("/top-b"));
        }
        for (int i = 0; i < 90; i++) {
            mvc.perform(get("/top-a"));
        }
        mvc.perform(get("/links/top").param("limit", "2"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("top-b"))
                .andExpect(jsonPath("$[1].code").value("top-a"));
    }
}
