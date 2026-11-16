package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.library.App;
import dojo.library.Library;
import dojo.library.domain.Book;
import dojo.library.domain.Loan;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LibraryTest {
    @Test
    void aLoanIsDueInTwoWeeksAndTheMemberIsTold() {
        Clock firstOfMarch = Clock.fixed(Instant.parse("2026-03-01T10:00:00Z"), ZoneOffset.UTC);
        List<String> sent = new ArrayList<>();
        Library library = App.create(firstOfMarch, (member, message) -> sent.add(member + ": " + message));
        library.books().add(new Book("978-0", "Domain-Driven Design"));

        Loan loan = library.borrowBook().borrow("ada", "978-0");

        assertEquals(LocalDate.parse("2026-03-15"), loan.dueOn());
        assertEquals(List.of("ada: Due back on 2026-03-15"), sent);
    }
}
