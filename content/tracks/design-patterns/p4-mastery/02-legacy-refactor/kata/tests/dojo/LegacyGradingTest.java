package dojo;

import static dojo.GradingSupport.allClasses;
import static dojo.GradingSupport.bodyLengths;
import static dojo.GradingSupport.code;
import static dojo.GradingSupport.rawSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacyGradingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 2, 2);

    private LibraryService library() {
        var library = new LibraryService();
        for (int i = 1; i <= 12; i++) {
            library.addBook("b" + i, "Book " + i);
        }
        library.addMember("sam", "student");
        library.addMember("pat", "staff");
        library.addMember("gus", "guest");
        return library;
    }

    @Test
    void registration_is_validated_as_before() {
        var library = library();
        assertMessage(IllegalArgumentException.class, "book exists: b1", () -> library.addBook("b1", "Again"));
        assertMessage(IllegalArgumentException.class, "isbn is required", () -> library.addBook(" ", "X"));
        assertMessage(IllegalArgumentException.class, "title is required", () -> library.addBook("b99", null));
        assertMessage(IllegalArgumentException.class, "unknown member type: alien", () -> library.addMember("al", "alien"));
        assertMessage(IllegalArgumentException.class, "member exists: sam", () -> library.addMember("sam", "guest"));
        assertMessage(IllegalArgumentException.class, "member id is required", () -> library.addMember("", "guest"));
    }

    @Test
    void loan_limits_and_periods_per_type() {
        var library = library();
        for (int i = 1; i <= 3; i++) {
            library.borrow("sam", "b" + i, DAY);
        }
        assertMessage(IllegalStateException.class, "loan limit reached: sam", () -> library.borrow("sam", "b4", DAY));
        library.borrow("gus", "b4", DAY);
        assertMessage(IllegalStateException.class, "loan limit reached: gus", () -> library.borrow("gus", "b5", DAY));
        for (int i = 5; i <= 12; i++) {
            assertEquals(DAY.plusDays(28), library.borrow("pat", "b" + i, DAY));
        }
        assertMessage(IllegalStateException.class, "already on loan: b1", () -> library.borrow("pat", "b1", DAY));
        assertMessage(IllegalArgumentException.class, "unknown member: zed", () -> library.borrow("zed", "b1", DAY));
        assertMessage(IllegalArgumentException.class, "unknown book: b77", () -> library.borrow("pat", "b77", DAY));
    }

    @Test
    void fees_by_type_with_the_student_cap() {
        var library = library();
        library.borrow("sam", "b1", DAY);
        assertEquals(500, library.giveBack("sam", "b1", DAY.plusDays(14 + 80)));
        library.borrow("pat", "b2", DAY);
        assertEquals(0, library.giveBack("pat", "b2", DAY.plusDays(300)));
        library.borrow("gus", "b3", DAY);
        assertEquals(0, library.giveBack("gus", "b3", DAY.plusDays(7)));
        library.borrow("gus", "b3", DAY);
        assertEquals(1000, library.giveBack("gus", "b3", DAY.plusDays(27)));
        assertEquals(1000, library.fines("gus"));
        assertEquals("gus: \"Book 3\" was 20 days late, fee 10.00", library.outbox().getLast());
        assertMessage(IllegalStateException.class, "gus has not borrowed b3", () -> library.giveBack("gus", "b3", DAY));
    }

    @Test
    void unpaid_fines_block_borrowing_until_paid() {
        var library = library();
        library.borrow("gus", "b1", DAY);
        library.giveBack("gus", "b1", DAY.plusDays(18));
        assertEquals(550, library.fines("gus"));
        assertMessage(IllegalStateException.class, "unpaid fines: gus", () -> library.borrow("gus", "b2", DAY));
        assertMessage(IllegalArgumentException.class, "payment exceeds fines: 550", () -> library.payFine("gus", 600));
        assertMessage(IllegalArgumentException.class, "payment must be positive", () -> library.payFine("gus", 0));
        library.payFine("gus", 50);
        assertEquals(500, library.fines("gus"));
        assertEquals(DAY.plusDays(7), library.borrow("gus", "b2", DAY), "exactly 5.00 owed still allows a loan");
        assertMessage(IllegalArgumentException.class, "unknown member: nobody", () -> library.fines("nobody"));
    }

    @Test
    void the_overdue_report_is_sorted_by_member_then_isbn() {
        var library = library();
        library.borrow("pat", "b9", DAY);
        library.borrow("sam", "b2", DAY);
        library.borrow("sam", "b1", DAY);
        library.borrow("gus", "b5", DAY);
        assertEquals("Overdue on 2026-03-04:\n- gus Book 5 (23 days)\n- pat Book 9 (2 days)\n- sam Book 1 (16 days)\n- sam Book 2 (16 days)",
                library.overdueReport(DAY.plusDays(30)));
        assertEquals("Nothing overdue", library.overdueReport(DAY.plusDays(7)));
    }

    @Test
    void member_types_are_a_type_not_strings() {
        assertTrue(MemberType.class.isEnum(), "MemberType should be an enum");
        assertEquals(List.of("STUDENT", "STAFF", "GUEST"),
                java.util.Arrays.stream(MemberType.values()).map(Enum::name).toList());
        assertEquals(MemberType.STAFF, MemberType.parse("staff"));
        assertEquals(3, MemberType.STUDENT.maxLoans());
        assertEquals(28, MemberType.STAFF.loanDays());
        assertEquals(500, MemberType.STUDENT.lateFeeCents(51));
        assertEquals(350, MemberType.GUEST.lateFeeCents(7));
        assertThrows(IllegalArgumentException.class, () -> MemberType.parse("Student "));
        int mentions = 0;
        for (String name : allClasses()) {
            mentions += GradingSupport.count(rawSource(name), "\"student\"");
        }
        assertTrue(mentions <= 1, "the text \"student\" should appear at most once (when parsing); found " + mentions);
    }

    @Test
    void no_long_methods_and_no_primitive_obsession() {
        for (String name : allClasses()) {
            String code = code(name);
            int longest = bodyLengths(code).stream().mapToInt(Integer::intValue).max().orElse(0);
            assertTrue(longest <= 15, name + " has a method of " + longest + " lines; keep each under 16");
            assertFalse(code.contains("Object[]") || code.contains("String[]"),
                    name + ": replace arrays used as records with real types");
            assertFalse(code.contains("instanceof"), name + ": no instanceof");
        }
        String service = code("dojo.LibraryService");
        long lines = service.lines().filter(line -> !line.isBlank()).count();
        assertTrue(lines <= 110, "LibraryService still has " + lines + " lines; move rules out of it (at most 110)");
        assertFalse(service.contains(".equals(\"\")"), "LibraryService should not compare strings to literals");
        assertFalse(service.contains("String.format"), "LibraryService should not format money itself");
    }

    private static void assertMessage(Class<? extends Throwable> type, String message, Runnable action) {
        assertEquals(message, assertThrows(type, action::run).getMessage());
    }
}
