package dojo.rooms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Small helpers shared by the contract tests of every stage. */
final class RoomsApi {
    private RoomsApi() {
    }

    /** A room name no other test uses: the tests share one application. */
    static String unique(String name) {
        return name + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static ResultActions postJson(MockMvc mvc, String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    static long createRoom(MockMvc mvc, String name, int capacity) throws Exception {
        String body = postJson(mvc, "/rooms", "{\"name\": \"" + name + "\", \"capacity\": " + capacity + "}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(body);
    }

    static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
