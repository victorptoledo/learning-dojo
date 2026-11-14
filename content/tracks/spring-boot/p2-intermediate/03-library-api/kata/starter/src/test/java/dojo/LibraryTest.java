package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryTest {
    @Autowired
    MockMvc mvc;

    ResultActions add(String isbn, String title, int year, String author) throws Exception {
        return mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(
                "{\"isbn\": \"%s\", \"title\": \"%s\", \"year\": %d, \"author\": \"%s\"}".formatted(isbn, title, year, author)));
    }

    @Test
    void a_book_can_be_added_and_read() throws Exception {
        add("isbn-v1", "Emma", 1815, "Jane Austen").andExpect(status().isCreated());
        mvc.perform(get("/books/isbn-v1")).andExpect(jsonPath("$.author").value("Jane Austen"));
    }
}
