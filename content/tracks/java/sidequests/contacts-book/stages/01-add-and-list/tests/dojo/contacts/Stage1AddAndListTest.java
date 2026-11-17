package dojo.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Stage1AddAndListTest {

    @TempDir
    Path dir;

    private Path book() {
        return dir.resolve("book.txt");
    }

    @Test
    void anEmptyBookSaysSo() {
        var result = Cli.run(book(), "list");
        assertEquals(0, result.code(), result.err());
        assertEquals(List.of("no contacts"), result.lines());
    }

    @Test
    void addConfirmsWithTheName() {
        var result = Cli.run(book(), "add", "Ana Lima", "ana@example.com", "+55 11 91234-5678");
        assertEquals(0, result.code(), result.err());
        assertEquals(List.of("added Ana Lima"), result.lines());
    }

    @Test
    void aContactAddedInOneRunIsListedInTheNext() {
        Cli.run(book(), "add", "Ana Lima", "ana@example.com", "+55 11 91234-5678");
        var result = Cli.run(book(), "list");
        assertEquals(0, result.code(), result.err());
        assertEquals(List.of("Ana Lima <ana@example.com> +55 11 91234-5678"), result.lines());
    }

    @Test
    void thePhoneIsOptional() {
        Cli.run(book(), "add", "Bruno", "bruno@example.com");
        assertEquals(List.of("Bruno <bruno@example.com>"), Cli.run(book(), "list").lines());
    }

    @Test
    void contactsAreSortedByNameIgnoringCase() {
        Cli.run(book(), "add", "carla", "carla@example.com");
        Cli.run(book(), "add", "Ana", "ana@example.com");
        Cli.run(book(), "add", "bruno", "bruno@example.com", "555");
        assertEquals(
                List.of("Ana <ana@example.com>", "bruno <bruno@example.com> 555", "carla <carla@example.com>"),
                Cli.run(book(), "list").lines());
    }

    @Test
    void manyRunsKeepEveryContact() {
        for (int i = 0; i < 5; i++) {
            Cli.run(book(), "add", "Person " + i, "p" + i + "@example.com");
        }
        assertEquals(5, Cli.run(book(), "list").lines().size());
    }

    @Test
    void separateFilesAreSeparateBooks() {
        Cli.run(book(), "add", "Ana", "ana@example.com");
        var other = Cli.run(dir.resolve("other.txt"), "list");
        assertEquals(List.of("no contacts"), other.lines());
    }
}
