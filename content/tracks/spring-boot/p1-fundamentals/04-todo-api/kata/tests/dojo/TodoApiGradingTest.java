package dojo;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The context (and the in-memory todos) is shared by every test, so tests use their own todos. */
@SpringBootTest
@AutoConfigureMockMvc
class TodoApiGradingTest {
    @Autowired
    MockMvc mvc;

    /** Creates a todo and returns its id, read from the JSON response. */
    long create(String title) throws Exception {
        String json = mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void create_answers_201_with_the_location_and_the_todo() throws Exception {
        mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"  write tests \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/todos/" + (create("probe") - 1))))
                .andExpect(jsonPath("$.title").value("write tests"))
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void reads_one_todo_and_404_for_an_unknown_id() throws Exception {
        long id = create("read me");
        mvc.perform(get("/todos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("read me"));
        mvc.perform(get("/todos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void lists_todos_in_id_order() throws Exception {
        long first = create("list a");
        long second = create("list b");
        String json = mvc.perform(get("/todos")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Number> ids = JsonPath.read(json, "$[*].id");
        var longs = ids.stream().map(Number::longValue).toList();
        org.junit.jupiter.api.Assertions.assertTrue(longs.indexOf(first) < longs.indexOf(second));
        org.junit.jupiter.api.Assertions.assertEquals(longs.stream().sorted().toList(), longs);
    }

    @Test
    void update_replaces_title_and_state() throws Exception {
        long id = create("draft");
        mvc.perform(put("/todos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"final\", \"done\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("final"))
                .andExpect(jsonPath("$.done").value(true));
        mvc.perform(get("/todos/" + id)).andExpect(jsonPath("$.done").value(true));
        mvc.perform(put("/todos/999999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"x\", \"done\": false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filters_by_done() throws Exception {
        long open = create("filter open");
        long finished = create("filter done");
        mvc.perform(put("/todos/" + finished).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"filter done\", \"done\": true}"));
        mvc.perform(get("/todos").param("done", "true"))
                .andExpect(jsonPath("$[*].id", hasItem((int) finished)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) open))));
        mvc.perform(get("/todos").param("done", "false"))
                .andExpect(jsonPath("$[*].id", hasItem((int) open)))
                .andExpect(jsonPath("$[*].done", not(hasItem(true))));
    }

    @Test
    void delete_answers_204_then_the_todo_is_gone() throws Exception {
        long id = create("delete me");
        mvc.perform(delete("/todos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/todos/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete("/todos/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void blank_titles_are_bad_requests() throws Exception {
        mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"  \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/todos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        long id = create("keep");
        mvc.perform(put("/todos/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\", \"done\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ids_are_never_reused() throws Exception {
        long id = create("short lived");
        mvc.perform(delete("/todos/" + id));
        org.junit.jupiter.api.Assertions.assertTrue(create("next") > id);
    }
}
