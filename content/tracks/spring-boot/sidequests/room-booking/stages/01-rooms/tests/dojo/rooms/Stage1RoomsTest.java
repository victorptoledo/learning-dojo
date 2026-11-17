package dojo.rooms;

import static dojo.rooms.RoomsApi.createRoom;
import static dojo.rooms.RoomsApi.postJson;
import static dojo.rooms.RoomsApi.unique;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class Stage1RoomsTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_new_room_answers_201_with_its_location() throws Exception {
        String name = unique("Ada");
        String location = postJson(mvc, "/rooms", "{\"name\": \"" + name + "\", \"capacity\": 8}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.capacity").value(8))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/rooms/\\d+"), "Location was " + location);

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.capacity").value(8));
    }

    @Test
    void names_are_stored_without_the_spaces_around_them() throws Exception {
        String name = unique("Turing");
        postJson(mvc, "/rooms", "{\"name\": \"   " + name + "  \", \"capacity\": 4}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void the_list_has_every_room_by_id() throws Exception {
        long first = createRoom(mvc, unique("Lovelace"), 6);
        long second = createRoom(mvc, unique("Hopper"), 10);
        String json = mvc.perform(get("/rooms")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(json, "$[*].id");
        List<Long> longs = ids.stream().map(Number::longValue).toList();
        assertTrue(longs.contains(first) && longs.contains(second), "missing rooms in " + json);
        assertTrue(longs.equals(longs.stream().sorted().toList()), "rooms are not sorted by id: " + json);
    }

    @Test
    void a_bad_room_is_400_with_the_bad_fields() throws Exception {
        postJson(mvc, "/rooms", "{\"name\": \"   \", \"capacity\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").isString())
                .andExpect(jsonPath("$.errors.capacity").isString());
        postJson(mvc, "/rooms", "{\"name\": \"" + "x".repeat(41) + "\", \"capacity\": 101}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isString())
                .andExpect(jsonPath("$.errors.capacity").isString());
        postJson(mvc, "/rooms", "{\"name\": \"" + unique("NoSize") + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.capacity").isString())
                .andExpect(jsonPath("$.errors.name").doesNotExist());
    }

    @Test
    void a_body_that_is_not_json_is_a_problem_too() throws Exception {
        postJson(mvc, "/rooms", "{\"name\": ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void names_are_unique_ignoring_case() throws Exception {
        String name = unique("Curie");
        createRoom(mvc, name, 5);
        postJson(mvc, "/rooms", "{\"name\": \"" + name.toUpperCase() + "\", \"capacity\": 5}")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void an_unknown_room_is_404() throws Exception {
        mvc.perform(get("/rooms/987654"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("room 987654 not found"));
        mvc.perform(delete("/rooms/987654")).andExpect(status().isNotFound());
    }

    @Test
    void a_deleted_room_is_gone() throws Exception {
        long id = createRoom(mvc, unique("Temporary"), 3);
        mvc.perform(delete("/rooms/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/rooms/" + id)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("room " + id + " not found"));
    }
}
