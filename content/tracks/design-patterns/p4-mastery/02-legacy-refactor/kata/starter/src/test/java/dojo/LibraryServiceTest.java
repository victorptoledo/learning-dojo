package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Characterisation tests: they pin today's behaviour, so they pass before and after the
 * refactoring. Keep them green after every small step.
 */
class LibraryServiceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 1, 5);

    private LibraryService library() {
        var library = new LibraryService();
        library.addBook("111", "Dune");
        library.addBook("222", "Emma");
        library.addMember("sam", "student");
        library.addMember("pat", "staff");
        library.addMember("gus", "guest");
        return library;
    }

    @Test
    void loan_periods_depend_on_the_member_type() {
        var library = library();
        assertEquals(DAY.plusDays(14), library.borrow("sam", "111", DAY));
        assertEquals(DAY.plusDays(7), library.borrow("gus", "222", DAY));
        assertEquals(List.of("sam: you borrowed \"Dune\", due 2026-01-19", "gus: you borrowed \"Emma\", due 2026-01-12"),
                library.outbox());
    }

    @Test
    void late_returns_cost_by_member_type() {
        var library = library();
        library.borrow("sam", "111", DAY);
        assertEquals(30, library.giveBack("sam", "111", DAY.plusDays(17)));
        library.borrow("gus", "111", DAY);
        assertEquals(150, library.giveBack("gus", "111", DAY.plusDays(10)));
        assertEquals(30, library.fines("sam"));
        assertEquals("gus: \"Dune\" was 3 days late, fee 1.50", library.outbox().getLast());
    }

    @Test
    void the_overdue_report_lists_late_loans() {
        var library = library();
        library.borrow("gus", "111", DAY);
        library.borrow("pat", "222", DAY);
        assertEquals("Overdue on 2026-01-20:\n- gus Dune (8 days)", library.overdueReport(DAY.plusDays(15)));
        assertEquals("Nothing overdue", library.overdueReport(DAY));
        assertThrows(IllegalStateException.class, () -> library.borrow("sam", "111", DAY));
    }
}
