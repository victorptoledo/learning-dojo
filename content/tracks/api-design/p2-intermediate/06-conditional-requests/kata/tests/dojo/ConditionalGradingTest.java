package dojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ConditionalGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired
    MockMvc mvc;

    record Created(String id, String etag) {}

    Created create(String title) throws Exception {
        MockHttpServletResponse response = mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\", \"body\": \"first draft\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andReturn().getResponse();
        return new Created(JsonPath.read(response.getContentAsString(), "$.id"), response.getHeader("ETag"));
    }

    String etagOf(String id) throws Exception {
        return mvc.perform(get("/documents/" + id)).andReturn().getResponse().getHeader("ETag");
    }

    ResultActions replace(String id, String ifMatch, String title) throws Exception {
        var request = put("/documents/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"" + title + "\", \"body\": \"edited\"}");
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        return mvc.perform(request);
    }

    static ResultActions isProblem(ResultActions actions, int status, String type) throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPES + type));
    }

    @Test
    void a_read_carries_a_strong_etag_last_modified_and_a_revalidate_policy() throws Exception {
        mvc.perform(get("/documents/readme"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", matchesPattern("\"[^\"]+\"")))
                .andExpect(header().string("Last-Modified", "Wed, 01 Apr 2026 12:00:00 GMT"))
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(jsonPath("$.title").value("README"));
    }

    @Test
    void if_none_match_with_the_current_etag_is_a_304_without_a_body() throws Exception {
        String etag = etagOf("changelog");
        mvc.perform(get("/documents/changelog").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(content().string(""));
    }

    @Test
    void if_none_match_takes_lists_weak_tags_and_a_star() throws Exception {
        String etag = etagOf("readme");
        mvc.perform(get("/documents/readme").header("If-None-Match", "\"nope\", " + etag))
                .andExpect(status().isNotModified());
        mvc.perform(get("/documents/readme").header("If-None-Match", "W/" + etag))
                .andExpect(status().isNotModified());
        mvc.perform(get("/documents/readme").header("If-None-Match", "*"))
                .andExpect(status().isNotModified());
        mvc.perform(get("/documents/readme").header("If-None-Match", "\"nope\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("readme"));
    }

    @Test
    void if_modified_since_works_when_there_is_no_if_none_match() throws Exception {
        String lastModified = mvc.perform(get("/documents/license")).andReturn().getResponse()
                .getHeader("Last-Modified");
        String dayBefore = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                Instant.parse("2026-03-31T12:00:00Z").atZone(ZoneOffset.UTC));
        mvc.perform(get("/documents/license").header("If-Modified-Since", lastModified))
                .andExpect(status().isNotModified());
        mvc.perform(get("/documents/license").header("If-Modified-Since", dayBefore))
                .andExpect(status().isOk());
        mvc.perform(get("/documents/license")
                        .header("If-Modified-Since", lastModified)
                        .header("If-None-Match", "\"nope\""))
                .andExpect(status().isOk());
    }

    @Test
    void a_change_without_if_match_is_a_428_and_changes_nothing() throws Exception {
        Created doc = create("Plan");
        isProblem(replace(doc.id(), null, "Sneaky"), 428, "precondition-required");
        mvc.perform(get("/documents/" + doc.id())).andExpect(jsonPath("$.title").value("Plan"));
    }

    @Test
    void a_change_with_the_current_etag_succeeds_and_gets_a_new_etag() throws Exception {
        Created doc = create("Notes");
        String newEtag = replace(doc.id(), doc.etag(), "Notes v2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Notes v2"))
                .andReturn().getResponse().getHeader("ETag");
        assertThat(newEtag, not(is(doc.etag())));
        mvc.perform(get("/documents/" + doc.id()).header("If-None-Match", doc.etag()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", newEtag));
    }

    @Test
    void the_second_of_two_concurrent_editors_gets_a_412_instead_of_a_lost_update() throws Exception {
        Created doc = create("Shared");
        String readByAlice = etagOf(doc.id());
        String readByBob = etagOf(doc.id());
        replace(doc.id(), readByAlice, "Alice's edit").andExpect(status().isOk());
        isProblem(replace(doc.id(), readByBob, "Bob's edit"), 412, "precondition-failed");
        mvc.perform(get("/documents/" + doc.id())).andExpect(jsonPath("$.title").value("Alice's edit"));
    }

    @Test
    void if_match_compares_strongly_and_accepts_a_star() throws Exception {
        Created doc = create("Strong");
        isProblem(replace(doc.id(), "W/" + doc.etag(), "Weak"), 412, "precondition-failed");
        replace(doc.id(), "\"other\", " + doc.etag(), "Listed").andExpect(status().isOk());
        replace(doc.id(), "*", "Any").andExpect(status().isOk());
    }

    @Test
    void delete_needs_the_current_etag_too() throws Exception {
        Created doc = create("Temporary");
        isProblem(mvc.perform(delete("/documents/" + doc.id())), 428, "precondition-required");
        replace(doc.id(), doc.etag(), "Temporary v2").andExpect(status().isOk());
        isProblem(mvc.perform(delete("/documents/" + doc.id()).header("If-Match", doc.etag())), 412,
                "precondition-failed");
        mvc.perform(delete("/documents/" + doc.id()).header("If-Match", etagOf(doc.id())))
                .andExpect(status().isNoContent());
        isProblem(mvc.perform(get("/documents/" + doc.id())), 404, "document-not-found");
    }
}
