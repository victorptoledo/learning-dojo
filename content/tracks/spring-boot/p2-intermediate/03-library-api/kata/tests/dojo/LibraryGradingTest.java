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

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class LibraryGradingTest {
    @Autowired
    MockMvc mvc;

    ResultActions add(String isbn, String title, int year, String author) throws Exception {
        return mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(
                "{\"isbn\": \"%s\", \"title\": \"%s\", \"year\": %d, \"author\": \"%s\"}".formatted(isbn, title, year, author)));
    }

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    AuthorRepository authors;

    long statementsDuring(ThrowingRunnable action) throws Exception {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    void adds_a_book_and_reuses_its_author() throws Exception {
        add("isbn-a1", "Emma", 1815, "Jane Austen").andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Jane Austen"));
        add("isbn-a2", "Persuasion", 1817, " Jane Austen ").andExpect(status().isCreated());
        assertEquals(1, authors.findAll().stream().filter(a -> a.getName().equals("Jane Austen")).count());
        mvc.perform(get("/books/isbn-a2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Persuasion"))
                .andExpect(jsonPath("$.year").value(1817));
    }

    @Test
    void duplicate_isbn_is_409_and_unknown_isbn_404() throws Exception {
        add("isbn-d1", "Dune", 1965, "Frank Herbert").andExpect(status().isCreated());
        add("isbn-d1", "Dune again", 1965, "Frank Herbert")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("book isbn-d1 already exists"));
        mvc.perform(get("/books/none")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("book none not found"));
    }

    @Test
    void invalid_books_are_400() throws Exception {
        add(" ", "", 2000, "x").andExpect(status().isBadRequest());
    }

    @Test
    void search_ignores_case_sorts_by_title_and_runs_one_query() throws Exception {
        add("isbn-s1", "The Hobbit", 1937, "Tolkien Search");
        add("isbn-s2", "Hobbit Tales", 2000, "Other Search");
        add("isbn-s3", "Unrelated", 2001, "Third Search");
        long statements = statementsDuring(() -> mvc.perform(get("/books").param("q", "HOBBIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Hobbit Tales"))
                .andExpect(jsonPath("$[0].author").value("Other Search"))
                .andExpect(jsonPath("$[1].title").value("The Hobbit")));
        assertEquals(1, statements, "search ran " + statements + " SQL statements: fetch the authors in the same query (N+1)");
    }

    @Test
    void books_of_an_author_come_in_pages_by_year_then_title() throws Exception {
        add("isbn-p1", "C", 2003, "Paged Author");
        add("isbn-p2", "B", 2001, "Paged Author");
        add("isbn-p3", "A", 2001, "Paged Author");
        add("isbn-p4", "Z", 1999, "Someone Else");
        mvc.perform(get("/authors/Paged Author/books").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("A"))
                .andExpect(jsonPath("$.items[1].title").value("B"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.size").value(2));
        mvc.perform(get("/authors/Paged Author/books").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("C"))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void an_author_page_does_not_load_authors_one_by_one() throws Exception {
        for (int i = 0; i < 5; i++) {
            add("isbn-n" + i, "Book " + i, 2000 + i, "N Plus One");
        }
        long statements = statementsDuring(() -> mvc.perform(get("/authors/N Plus One/books").param("size", "5"))
                .andExpect(jsonPath("$.items.length()").value(5)));
        assertTrue(statements <= 2, "the page ran " + statements + " SQL statements (one for the page, one to count)");
    }

    @Test
    void stats_count_books_per_author_most_first() throws Exception {
        add("isbn-t1", "One", 2000, "Stats Zed");
        add("isbn-t2", "Two", 2000, "Stats Zed");
        add("isbn-t3", "Three", 2000, "Stats Amy");
        add("isbn-t4", "Four", 2000, "Stats Amy");
        add("isbn-t5", "Five", 2000, "Stats Bob");
        String json = mvc.perform(get("/authors/stats")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<String> names = com.jayway.jsonpath.JsonPath.read(json, "$[*].author");
        var mine = names.stream().filter(n -> n.startsWith("Stats ")).toList();
        assertEquals(java.util.List.of("Stats Amy", "Stats Zed", "Stats Bob"), mine);
        mvc.perform(get("/authors/stats")).andExpect(jsonPath("$[?(@.author == 'Stats Amy')].books").value(2));
    }
}
