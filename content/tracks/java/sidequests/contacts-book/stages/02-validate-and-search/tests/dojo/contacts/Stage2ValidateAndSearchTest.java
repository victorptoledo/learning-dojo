package dojo.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Stage2ValidateAndSearchTest {

    @TempDir
    Path dir;

    private Path book;

    @BeforeEach
    void someContacts() {
        book = dir.resolve("book.txt");
        Cli.run(book, "add", "Ana Lima", "ana@example.com", "111");
        Cli.run(book, "add", "Bruno Costa", "bruno@work.org");
    }

    @Test
    void missingOrUnknownCommandsPrintUsage() {
        for (var result : List.of(
                Cli.run(book),
                Cli.run(book, "fly"),
                Cli.run(book, "add", "Only Name"),
                Cli.run(book, "find"),
                Cli.runRaw("list"))) {
            assertEquals(2, result.code(), result.err());
            assertTrue(result.err().startsWith("usage: contacts"), result.err());
        }
    }

    @Test
    void aBadEmailIsRejectedAndNothingIsSaved() {
        for (String email : List.of("plain", "two@@example.com", "no@dot", "sp ace@example.com", "a@b@c.com")) {
            var result = Cli.run(book, "add", "Someone", email);
            assertEquals(1, result.code(), email);
            assertEquals("invalid email: " + email, result.err().strip());
        }
        assertEquals(2, Cli.run(book, "list").lines().size());
    }

    @Test
    void aDuplicateEmailIgnoringCaseIsRejected() {
        var result = Cli.run(book, "add", "Other Ana", "ANA@example.com");
        assertEquals(1, result.code());
        assertEquals("duplicate email: ANA@example.com", result.err().strip());
        assertEquals(2, Cli.run(book, "list").lines().size());
    }

    @Test
    void findMatchesNameOrEmailIgnoringCase() {
        assertEquals(List.of("Ana Lima <ana@example.com> 111"), Cli.run(book, "find", "LIMA").lines());
        assertEquals(List.of("Bruno Costa <bruno@work.org>"), Cli.run(book, "find", "work.org").lines());
        var both = Cli.run(book, "find", "a");
        assertEquals(0, both.code());
        assertEquals(2, both.lines().size());
    }

    @Test
    void findWithoutAMatchExitsWith1() {
        var result = Cli.run(book, "find", "zzz");
        assertEquals(1, result.code());
        assertEquals(List.of(), result.lines());
    }

    @Test
    void removeDeletesByEmailIgnoringCase() {
        var result = Cli.run(book, "remove", "BRUNO@work.org");
        assertEquals(0, result.code(), result.err());
        assertEquals(List.of("removed Bruno Costa"), result.lines());
        assertEquals(List.of("Ana Lima <ana@example.com> 111"), Cli.run(book, "list").lines());
    }

    @Test
    void removingAnUnknownEmailFails() {
        var result = Cli.run(book, "remove", "ghost@example.com");
        assertEquals(1, result.code());
        assertEquals("no contact with email ghost@example.com", result.err().strip());
    }

    @Test
    void namesWithPunctuationComeBackExactly() {
        String name = "Lima, Ana \"Nana\"; da Silva";
        assertEquals(0, Cli.run(book, "add", name, "nana@example.com").code());
        var found = Cli.run(book, "find", "nana@");
        assertEquals(List.of(name + " <nana@example.com>"), found.lines());
    }
}
