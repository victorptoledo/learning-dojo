package dojo;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Each test starts from the two original articles. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HttpGradingTest {
    @Autowired
    MockMvc mvc;

    @Test
    void get_answers_json_with_an_etag_and_cache_control() throws Exception {
        mvc.perform(get("/articles/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("ETag", "\"v1\""))
                .andExpect(header().string("Cache-Control", "max-age=60"))
                .andExpect(jsonPath("$.title").value("Hello, HTTP"));
    }

    @Test
    void a_matching_if_none_match_is_304_without_a_body() throws Exception {
        mvc.perform(get("/articles/2").header("If-None-Match", "\"v1\""))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", "\"v1\""))
                .andExpect(content().string(""));
        mvc.perform(get("/articles/2").header("If-None-Match", "\"v0\"")).andExpect(status().isOk());
    }

    @Test
    void head_gives_the_same_headers() throws Exception {
        // The real server drops the body of a HEAD response; MockMvc keeps it, so only headers are checked.
        mvc.perform(head("/articles/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"v1\""));
    }

    @Test
    void unknown_articles_are_404() throws Exception {
        mvc.perform(get("/articles/99")).andExpect(status().isNotFound());
        mvc.perform(put("/articles/99").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_creates_a_new_version_with_a_new_etag() throws Exception {
        mvc.perform(put("/articles/1").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"New\",\"body\":\"Text\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"v2\""))
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/articles/1").header("If-None-Match", "\"v1\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"));
    }

    @Test
    void put_with_a_stale_if_match_is_412_and_changes_nothing() throws Exception {
        mvc.perform(put("/articles/2").header("If-Match", "\"v7\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Lost\",\"body\":\"update\"}"))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(get("/articles/2")).andExpect(jsonPath("$.title").value("Caching"));
        mvc.perform(put("/articles/2").header("If-Match", "\"v1\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Kept\",\"body\":\"update\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void methods_and_content_types_that_are_not_supported_say_so() throws Exception {
        mvc.perform(delete("/articles/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
        mvc.perform(put("/articles/1").contentType(MediaType.TEXT_PLAIN).content("title=x"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
