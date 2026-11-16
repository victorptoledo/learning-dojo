package dojo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
class CommentsTest {
    @Autowired
    MockMvc mvc;

    @Test
    void a_user_posts_a_comment() throws Exception {
        mvc.perform(post("/api/articles/visible-post/comments").with(httpBasic("ada", "ada-pass"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"  First! \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.author").value("ada"))
                .andExpect(jsonPath("$.body").value("First!"));
    }

    @Test
    void an_article_without_comments_has_an_empty_last_page() throws Exception {
        mvc.perform(get("/api/articles/visible-empty/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty());
    }
}
