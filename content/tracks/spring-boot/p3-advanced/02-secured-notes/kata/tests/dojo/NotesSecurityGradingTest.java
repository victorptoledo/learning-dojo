package dojo;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class NotesSecurityGradingTest {
    @Autowired
    MockMvc mvc;

    static final RequestPostProcessor ADA = httpBasic("ada", "ada-pass");
    static final RequestPostProcessor LINUS = httpBasic("linus", "linus-pass");
    static final RequestPostProcessor ROOT = httpBasic("root", "root-pass");

    long write(RequestPostProcessor user, String text) throws Exception {
        String json = mvc.perform(post("/notes").with(user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void the_count_is_public() throws Exception {
        mvc.perform(get("/notes/count")).andExpect(status().isOk()).andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    void notes_need_a_login_and_a_right_password() throws Exception {
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notes").with(httpBasic("ada", "wrong"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/notes").contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void a_note_belongs_to_the_user_who_wrote_it() throws Exception {
        long id = write(ADA, "ada's first");
        mvc.perform(get("/notes/" + id).with(ADA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("ada"))
                .andExpect(jsonPath("$.text").value("ada's first"));
        mvc.perform(get("/notes").with(ADA)).andExpect(jsonPath("$[*].owner", everyItem(is("ada"))));
    }

    @Test
    void other_users_notes_look_like_they_do_not_exist() throws Exception {
        long id = write(ADA, "private");
        mvc.perform(get("/notes/" + id).with(LINUS))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("note " + id + " not found"));
        mvc.perform(delete("/notes/" + id).with(LINUS)).andExpect(status().isNotFound());
        mvc.perform(get("/notes/" + id).with(ADA)).andExpect(status().isOk());
        mvc.perform(get("/notes").with(LINUS)).andExpect(jsonPath("$[*].owner", everyItem(is("linus"))));
    }

    @Test
    void owners_delete_their_notes() throws Exception {
        long id = write(LINUS, "temporary");
        mvc.perform(delete("/notes/" + id).with(LINUS)).andExpect(status().isNoContent());
        mvc.perform(get("/notes/" + id).with(LINUS)).andExpect(status().isNotFound());
    }

    @Test
    void the_admin_sees_and_deletes_every_note() throws Exception {
        long id = write(ADA, "for the admin");
        mvc.perform(get("/admin/notes").with(ROOT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
        mvc.perform(get("/notes/" + id).with(ROOT)).andExpect(status().isOk());
        mvc.perform(delete("/notes/" + id).with(ROOT)).andExpect(status().isNoContent());
    }

    @Test
    void users_are_forbidden_from_the_admin_area() throws Exception {
        mvc.perform(get("/admin/notes").with(ADA)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/notes")).andExpect(status().isUnauthorized());
    }

    @Test
    void blank_notes_are_rejected() throws Exception {
        mvc.perform(post("/notes").with(ADA).contentType(MediaType.APPLICATION_JSON).content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());
    }
}
