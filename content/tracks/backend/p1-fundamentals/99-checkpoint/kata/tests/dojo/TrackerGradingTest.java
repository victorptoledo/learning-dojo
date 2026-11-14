package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class TrackerGradingTest {
    static final MediaType MERGE_PATCH = MediaType.parseMediaType("application/merge-patch+json");

    @Autowired
    MockMvc mvc;

    void project(String key) throws Exception {
        mvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\": \"" + key + "\", \"name\": \"Project " + key + "\"}")).andExpect(status().isCreated());
    }

    ResultActions issue(String key, String json) throws Exception {
        return mvc.perform(post("/projects/" + key + "/issues").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions patchIssue(String path, String ifMatch, String json) throws Exception {
        var request = patch(path).contentType(MERGE_PATCH).content(json);
        if (ifMatch != null) {
            request = request.header("If-Match", ifMatch);
        }
        return mvc.perform(request);
    }

    @Test
    void issues_are_numbered_per_project() throws Exception {
        project("ALPHA");
        project("BETA");
        issue("ALPHA", "{\"title\": \"first\"}").andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/projects/ALPHA/issues/1")))
                .andExpect(jsonPath("$.key").value("ALPHA-1"));
        issue("ALPHA", "{\"title\": \"second\"}").andExpect(jsonPath("$.number").value(2));
        issue("BETA", "{\"title\": \"other\"}").andExpect(jsonPath("$.key").value("BETA-1"));
    }

    @Test
    void projects_and_issues_are_validated() throws Exception {
        mvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON).content("{\"key\": \"lower\", \"name\": \"x\"}"))
                .andExpect(status().isBadRequest());
        project("GAMMA");
        mvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON).content("{\"key\": \"GAMMA\", \"name\": \"again\"}"))
                .andExpect(status().isConflict());
        issue("GAMMA", "{\"title\": \" \"}").andExpect(status().isBadRequest());
        issue("NOPE", "{\"title\": \"t\"}").andExpect(status().isNotFound());
    }

    @Test
    void an_issue_has_its_version_as_etag() throws Exception {
        project("DELTA");
        issue("DELTA", "{\"title\": \"etag\"}").andExpect(header().string("ETag", "\"0\""));
        mvc.perform(get("/projects/DELTA/issues/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/projects/DELTA/issues/9")).andExpect(status().isNotFound());
    }

    @Test
    void patch_needs_if_match_and_refuses_stale_versions() throws Exception {
        project("EPS");
        issue("EPS", "{\"title\": \"lock me\"}");
        patchIssue("/projects/EPS/issues/1", null, "{\"status\": \"DONE\"}")
                .andExpect(status().isPreconditionRequired());
        patchIssue("/projects/EPS/issues/1", "\"0\"", "{\"status\": \"IN_PROGRESS\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        patchIssue("/projects/EPS/issues/1", "\"0\"", "{\"status\": \"DONE\"}")
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.detail").value("the issue changed; its version is now 1"));
        mvc.perform(get("/projects/EPS/issues/1")).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void merge_patch_sets_present_fields_and_null_clears() throws Exception {
        project("ZETA");
        issue("ZETA", "{\"title\": \"patch me\", \"assignee\": \"ada\"}");
        patchIssue("/projects/ZETA/issues/1", "\"0\"", "{\"title\": \"patched\"}")
                .andExpect(jsonPath("$.title").value("patched"))
                .andExpect(jsonPath("$.assignee").value("ada"))
                .andExpect(jsonPath("$.status").value("OPEN"));
        patchIssue("/projects/ZETA/issues/1", "\"1\"", "{\"assignee\": null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void bad_patches_are_400() throws Exception {
        project("ETA");
        issue("ETA", "{\"title\": \"strict\"}");
        patchIssue("/projects/ETA/issues/1", "\"0\"", "{\"status\": \"LATER\"}").andExpect(status().isBadRequest());
        patchIssue("/projects/ETA/issues/1", "\"0\"", "{\"number\": 7}").andExpect(status().isBadRequest());
        patchIssue("/projects/ETA/issues/1", "\"0\"", "{\"title\": \"\"}").andExpect(status().isBadRequest());
        mvc.perform(get("/projects/ETA/issues/1")).andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void lists_are_filtered_paginated_and_sorted_by_number() throws Exception {
        project("THETA");
        issue("THETA", "{\"title\": \"a\", \"assignee\": \"ada\"}");
        issue("THETA", "{\"title\": \"b\", \"assignee\": \"linus\"}");
        issue("THETA", "{\"title\": \"c\", \"assignee\": \"ada\"}");
        issue("THETA", "{\"title\": \"d\", \"assignee\": \"ada\"}");
        patchIssue("/projects/THETA/issues/4", "\"0\"", "{\"status\": \"DONE\"}");
        mvc.perform(get("/projects/THETA/issues").param("assignee", "ada").param("status", "OPEN"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].key").value("THETA-1"))
                .andExpect(jsonPath("$.items[1].key").value("THETA-3"));
        mvc.perform(get("/projects/THETA/issues").param("page", "1").param("size", "3"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].number").value(4))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void out_of_range_pages_and_unknown_filters_are_400() throws Exception {
        project("IOTA");
        mvc.perform(get("/projects/IOTA/issues").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/projects/IOTA/issues").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/projects/IOTA/issues").param("status", "SOMEDAY")).andExpect(status().isBadRequest());
        mvc.perform(get("/projects/NOPE/issues")).andExpect(status().isNotFound());
    }
}
