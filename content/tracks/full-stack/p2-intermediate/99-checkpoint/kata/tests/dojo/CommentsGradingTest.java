package dojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class CommentsGradingTest {
    static final RequestPostProcessor ADA = httpBasic("ada", "ada-pass");
    static final RequestPostProcessor LINUS = httpBasic("linus", "linus-pass");
    static final RequestPostProcessor ROOT = httpBasic("root", "root-pass");

    @Autowired
    MockMvc mvc;

    long comment(String slug, RequestPostProcessor user, String body) throws Exception {
        String json = mvc.perform(post("/api/articles/" + slug + "/comments").with(user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"" + body + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void the_created_item_has_a_location_an_instant_and_can_be_deleted_by_its_author() throws Exception {
        String json = mvc.perform(post("/api/articles/created/comments").with(LINUS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"Nice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canDelete").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        String createdAt = JsonPath.read(json, "$.createdAt");
        assertThat(Instant.parse(createdAt)).isBefore(Instant.now().plusSeconds(1));
        mvc.perform(get("/api/articles/created/comments"))
                .andExpect(jsonPath("$.items[0].id").value(id));
        assertThat(mvc.perform(post("/api/articles/created/comments").with(LINUS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"x\"}"))
                .andReturn().getResponse().getHeader("Location")).isEqualTo("/api/comments/" + (id + 1));
    }

    @Test
    void the_list_is_newest_first_and_only_for_that_article() throws Exception {
        long first = comment("ordering", ADA, "one");
        long second = comment("ordering", LINUS, "two");
        comment("other-article", ADA, "elsewhere");
        long third = comment("ordering", ADA, "three");
        mvc.perform(get("/api/articles/ordering/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", contains((int) third, (int) second, (int) first)))
                .andExpect(jsonPath("$.items[*].body", contains("three", "two", "one")))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void cursors_walk_every_page_exactly_once() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ids.add(comment("paged", ADA, "c" + i));
        }
        List<Long> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            String url = "/api/articles/paged/comments?limit=2" + (cursor == null ? "" : "&cursor=" + cursor);
            String json = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<Number> page = JsonPath.read(json, "$.items[*].id");
            assertThat(page.size()).isLessThanOrEqualTo(2);
            page.forEach(id -> seen.add(id.longValue()));
            cursor = JsonPath.read(json, "$.nextCursor");
            pages++;
        } while (cursor != null && pages < 10);
        assertThat(pages).isEqualTo(3);
        assertThat(seen).containsExactlyElementsOf(ids.reversed());
    }

    @Test
    void a_full_last_page_has_no_cursor_and_the_cursor_is_base64url_of_the_last_id() throws Exception {
        comment("exact", ADA, "a");
        long b = comment("exact", ADA, "b");
        String json = mvc.perform(get("/api/articles/exact/comments?limit=1"))
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(json, "$.nextCursor");
        assertThat(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)).isEqualTo(Long.toString(b));
        assertThat(cursor).doesNotContain("=");
        mvc.perform(get("/api/articles/exact/comments?limit=2")).andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void the_default_page_has_ten_items() throws Exception {
        for (int i = 0; i < 11; i++) {
            comment("busy", LINUS, "n" + i);
        }
        mvc.perform(get("/api/articles/busy/comments"))
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.nextCursor").isString());
    }

    @Test
    void bad_limits_and_cursors_are_400() throws Exception {
        mvc.perform(get("/api/articles/x/comments?limit=0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles/x/comments?limit=51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles/x/comments?cursor=bm90LWEtbnVtYmVy")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/articles/x/comments?cursor=!!!")).andExpect(status().isBadRequest());
    }

    @Test
    void a_blank_or_long_body_is_a_400_problem_that_names_the_field() throws Exception {
        mvc.perform(post("/api/articles/invalid/comments").with(ADA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.body").isString());
        mvc.perform(post("/api/articles/invalid/comments").with(ADA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").isString());
        mvc.perform(get("/api/articles/invalid/comments")).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void the_author_comes_from_the_login_not_the_request() throws Exception {
        mvc.perform(post("/api/articles/spoof/comments").with(LINUS)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"hi\", \"author\": \"root\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("linus"));
        mvc.perform(post("/api/articles/spoof/comments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"anonymous\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void can_delete_depends_on_who_is_looking() throws Exception {
        comment("viewers", ADA, "by ada");
        comment("viewers", LINUS, "by linus");
        mvc.perform(get("/api/articles/viewers/comments"))
                .andExpect(jsonPath("$.items[*].canDelete", everyItem(is(false))));
        mvc.perform(get("/api/articles/viewers/comments").with(ADA))
                .andExpect(jsonPath("$.items[*].canDelete", contains(false, true)));
        mvc.perform(get("/api/articles/viewers/comments").with(ROOT))
                .andExpect(jsonPath("$.items[*].canDelete", contains(true, true)));
    }

    @Test
    void only_the_author_or_an_admin_deletes() throws Exception {
        long mine = comment("deleting", ADA, "mine");
        long other = comment("deleting", ADA, "another");
        mvc.perform(delete("/api/comments/" + mine).with(LINUS)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/comments/" + mine).with(ADA)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/comments/" + other).with(ROOT)).andExpect(status().isNoContent());
        mvc.perform(get("/api/articles/deleting/comments")).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void deleting_an_unknown_comment_is_404_and_needs_a_login() throws Exception {
        mvc.perform(delete("/api/comments/987654").with(ADA)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/comments/987654")).andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }
}
