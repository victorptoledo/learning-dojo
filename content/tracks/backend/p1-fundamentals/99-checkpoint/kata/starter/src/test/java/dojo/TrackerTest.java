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

@SpringBootTest
@AutoConfigureMockMvc
class TrackerTest {
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
    void an_issue_is_created_in_a_project() throws Exception {
        project("VIS");
        issue("VIS", "{\"title\": \"hello\"}").andExpect(status().isCreated()).andExpect(jsonPath("$.key").value("VIS-1"));
    }
}
