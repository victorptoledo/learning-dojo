package dojo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestBeans.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StatementsTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    @Autowired
    EntryRepository entries;

    @Test
    void the_owner_reads_their_account() throws Exception {
        long id = accounts.save(new Account("ada", "Main", "USD", 12345, 50000)).getId();
        mvc.perform(get("/accounts/" + id).header("X-User", "ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Main"))
                .andExpect(jsonPath("$.balanceCents").value(12345));
    }

    @Test
    void someone_else_gets_a_404() throws Exception {
        long id = accounts.save(new Account("bob", "Savings", "USD", 100, 0)).getId();
        mvc.perform(get("/accounts/" + id).header("X-User", "ada")).andExpect(status().isNotFound());
    }

    @Test
    void the_statement_is_newest_first() throws Exception {
        long id = accounts.save(new Account("ada", "Main", "USD", 0, 0)).getId();
        entries.save(new Entry(id, LocalDate.parse("2026-11-01"), "Salary", 300000));
        entries.save(new Entry(id, LocalDate.parse("2026-11-03"), "Coffee", -450));
        mvc.perform(get("/accounts/" + id + "/entries").header("X-User", "ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Coffee"))
                .andExpect(jsonPath("$[1].description").value("Salary"));
    }
}
