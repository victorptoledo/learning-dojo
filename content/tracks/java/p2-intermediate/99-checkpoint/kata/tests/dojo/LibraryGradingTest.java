package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibraryGradingTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private final AtomicReference<LocalDate> today = new AtomicReference<>(START);
    private Library library;

    private static final Book DUNE = new Book("111", "Dune", "Herbert");
    private static final Book EMMA = new Book("222", "Emma", "Austen");
    private static final Book CARRIE = new Book("333", "Carrie", "King");

    @BeforeEach
    void setUp() {
        library = new Library(today::get);
        library.addBook(DUNE);
        library.addBook(EMMA);
        library.addBook(CARRIE);
        library.addMember(new Member("m1", "Ada", 2));
        library.addMember(new Member("m2", "Linus", 1));
    }

    @Test
    void repository_saves_replaces_and_keeps_first_saved_order() {
        var repository = new InMemoryRepository<String, Book>();
        repository.save(DUNE);
        repository.save(EMMA);
        repository.save(new Book("111", "Dune (2nd ed.)", "Herbert"));
        assertEquals(2, repository.count());
        assertEquals(List.of("Dune (2nd ed.)", "Emma"), repository.findAll().stream().map(Book::title).toList());
        assertEquals(Optional.of(EMMA), repository.findById("222"));
        assertEquals(Optional.empty(), repository.findById("999"));
    }

    @Test
    void repository_filters_deletes_and_returns_unmodifiable_lists() {
        var repository = new InMemoryRepository<String, Book>();
        repository.save(DUNE);
        repository.save(EMMA);
        assertEquals(List.of(EMMA), repository.findAll(book -> book.author().startsWith("A")));
        assertTrue(repository.delete("111"));
        assertFalse(repository.delete("111"));
        var all = repository.findAll();
        assertThrows(UnsupportedOperationException.class, () -> all.add(DUNE));
    }

    @Test
    void lending_creates_a_loan_due_in_fourteen_days() {
        var loan = library.lend("111", "m1");
        assertEquals(new Loan("111", "m1", START.plusDays(14)), loan);
        assertEquals(List.of(CARRIE, EMMA), library.available());
    }

    @Test
    void unknown_books_and_members_are_not_found() {
        var book = assertThrows(LibraryException.NotFound.class, () -> library.lend("999", "m1"));
        assertEquals("book 999 not found", book.getMessage());
        var member = assertThrows(LibraryException.NotFound.class, () -> library.lend("111", "m9"));
        assertEquals("member m9 not found", member.getMessage());
    }

    @Test
    void a_lent_book_is_not_available() {
        library.lend("111", "m1");
        var error = assertThrows(LibraryException.NotAvailable.class, () -> library.lend("111", "m2"));
        assertEquals("book 111 is already lent", error.getMessage());
    }

    @Test
    void members_cannot_exceed_their_loan_limit() {
        library.lend("111", "m2");
        var error = assertThrows(LibraryException.LoanLimitReached.class, () -> library.lend("222", "m2"));
        assertEquals("member m2 already has 1 loans", error.getMessage());
        library.lend("222", "m1");
        library.lend("333", "m1");
        assertEquals(List.of(), library.available());
    }

    @Test
    void giving_back_frees_the_book_and_unknown_loans_fail() {
        library.lend("111", "m2");
        library.giveBack("111");
        assertEquals(List.of(CARRIE, DUNE, EMMA), library.available());
        library.lend("222", "m2");
        var error = assertThrows(LibraryException.NotFound.class, () -> library.giveBack("111"));
        assertEquals("loan 111 not found", error.getMessage());
    }

    @Test
    void overdue_loans_are_the_ones_past_their_due_date_sorted() {
        library.lend("222", "m1");
        today.set(START.plusDays(3));
        library.lend("111", "m1");
        library.lend("333", "m2");
        today.set(START.plusDays(14));
        assertEquals(List.of(), library.overdue(), "due today is not overdue yet");
        today.set(START.plusDays(16));
        assertEquals(List.of(new Loan("222", "m1", START.plusDays(14))), library.overdue());
        today.set(START.plusDays(30));
        assertEquals(List.of("222", "111", "333"), library.overdue().stream().map(Loan::isbn).toList());
    }

    @Test
    void titles_by_member_are_sorted_both_ways() {
        library.lend("222", "m1");
        library.lend("111", "m1");
        library.lend("333", "m2");
        assertEquals(Map.of("m1", List.of("Dune", "Emma"), "m2", List.of("Carrie")), library.titlesByMember());
        assertEquals(List.of("m1", "m2"), List.copyOf(library.titlesByMember().keySet()));
    }

    @Test
    void every_refusal_is_a_library_exception() {
        LibraryException error = assertThrows(LibraryException.class, () -> library.giveBack("111"));
        assertTrue(error instanceof LibraryException.NotFound);
    }
}
