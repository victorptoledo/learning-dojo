package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.library.App;
import dojo.library.Library;
import dojo.library.adapters.InMemoryLoans;
import dojo.library.domain.Book;
import dojo.library.domain.Books;
import dojo.library.domain.Loan;
import dojo.library.domain.LoanPolicy;
import dojo.library.domain.LoanRefusedException;
import dojo.library.domain.Loans;
import dojo.library.domain.Notifier;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LibraryGradingTest {
    private static final Map<String, Set<String>> LIBRARY = GradingGraph.scan("dojo.library");

    /** A clock the test moves forward. */
    static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-03-01T10:00:00Z");

        void advanceDays(long days) {
            now = now.plus(Duration.ofDays(days));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final List<String> sent = new ArrayList<>();
    private final Library library = App.create(clock, (member, message) -> sent.add(member + ": " + message));

    LibraryGradingTest() {
        for (String isbn : List.of("b1", "b2", "b3", "b4", "b5")) {
            library.books().add(new Book(isbn, "Book " + isbn));
        }
    }

    private static List<String> uses(String fromPrefix, java.util.function.Predicate<String> forbidden) {
        return LIBRARY.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(fromPrefix))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    private static boolean outerRing(String name) {
        return name.startsWith("dojo.library.adapters.") || name.equals("dojo.library.App")
                || name.equals("dojo.library.Library");
    }

    @Test
    void theDomainUsesOnlyItself() {
        assertTrue(LIBRARY.containsKey("dojo.library.domain.LoanPolicy"), "the scan found no library classes");
        assertEquals(List.of(), uses("dojo.library.domain.", to -> to.startsWith("dojo.library.application.")
                || outerRing(to) || to.equals("java.lang.System")));
    }

    @Test
    void theApplicationUsesNoAdapter() {
        assertEquals(List.of(), uses("dojo.library.application.", to -> outerRing(to) || to.equals("java.lang.System")));
    }

    @Test
    void thePortsAreInterfacesInTheDomain() {
        for (Class<?> port : List.of(Books.class, Loans.class, Notifier.class)) {
            assertTrue(port.isInterface(), port.getSimpleName() + " should be an interface");
        }
        assertTrue(Loans.class.isAssignableFrom(InMemoryLoans.class), "InMemoryLoans should implement Loans");
    }

    @Test
    void inMemoryLoansIsNoLongerAGlobal() {
        boolean singleton = Arrays.stream(InMemoryLoans.class.getDeclaredFields())
                .anyMatch(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == InMemoryLoans.class);
        assertFalse(singleton, "InMemoryLoans still has a static instance");
    }

    @Test
    void twoLibrariesDoNotShareLoans() {
        Library other = App.create(clock, (member, message) -> { });
        other.books().add(new Book("b1", "Book b1"));
        library.borrowBook().borrow("ada", "b1");
        other.borrowBook().borrow("bob", "b1");
    }

    @Test
    void aLoanRunsFourteenDaysFromTheClock() {
        Loan loan = library.borrowBook().borrow("ada", "b1");
        assertEquals(new Loan("b1", "ada", LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-15")), loan);
        assertEquals(List.of("ada: Due back on 2026-03-15"), sent);
    }

    @Test
    void unknownAndLentBooksAreRefused() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook().borrow("ada", "nope"));
        assertEquals("unknown book nope", unknown.getMessage());
        library.borrowBook().borrow("ada", "b1");
        LoanRefusedException lent = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook().borrow("bob", "b1"));
        assertEquals("already on loan: b1", lent.getMessage());
    }

    @Test
    void aFourthLoanIsRefused() {
        library.borrowBook().borrow("ada", "b1");
        library.borrowBook().borrow("ada", "b2");
        library.borrowBook().borrow("ada", "b3");
        LoanRefusedException refused = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook().borrow("ada", "b4"));
        assertEquals("member has 3 loans", refused.getMessage());
        library.borrowBook().borrow("bob", "b4");
    }

    @Test
    void anOverdueLoanBlocksNewLoansBeforeTheCountDoes() {
        library.borrowBook().borrow("ada", "b1");
        clock.advanceDays(14);
        library.borrowBook().borrow("ada", "b2");
        library.borrowBook().borrow("ada", "b3");
        clock.advanceDays(1);
        LoanRefusedException refused = assertThrows(LoanRefusedException.class,
                () -> library.borrowBook().borrow("ada", "b4"));
        assertEquals("member has an overdue loan", refused.getMessage());
    }

    @Test
    void anOnTimeReturnCostsNothingAndSendsNothing() {
        library.borrowBook().borrow("ada", "b1");
        sent.clear();
        clock.advanceDays(14);
        assertEquals(0, library.returnBook().giveBack("b1"));
        assertEquals(List.of(), sent);
        library.borrowBook().borrow("bob", "b1");
    }

    @Test
    void aLateReturnIsChargedAndTold() {
        library.borrowBook().borrow("ada", "b1");
        sent.clear();
        clock.advanceDays(17);
        assertEquals(75, library.returnBook().giveBack("b1"));
        assertEquals(List.of("ada: Late fee: 75 cents"), sent);
    }

    @Test
    void theLateFeeIsCapped() {
        library.borrowBook().borrow("ada", "b1");
        clock.advanceDays(200);
        assertEquals(1000, library.returnBook().giveBack("b1"));
    }

    @Test
    void returningABookThatIsNotLentFails() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> library.returnBook().giveBack("b1"));
        assertEquals("not on loan: b1", error.getMessage());
    }

    @Test
    void thePolicyIsPure() {
        LoanPolicy policy = new LoanPolicy();
        LocalDate today = LocalDate.parse("2026-01-10");
        assertEquals(LocalDate.parse("2026-01-24"), policy.dueDate(today));
        policy.checkCanBorrow(List.of(), today);
        Loan late = new Loan("x", "ada", LocalDate.parse("2025-12-01"), LocalDate.parse("2025-12-15"));
        assertThrows(LoanRefusedException.class, () -> policy.checkCanBorrow(List.of(late), today));
    }
}
