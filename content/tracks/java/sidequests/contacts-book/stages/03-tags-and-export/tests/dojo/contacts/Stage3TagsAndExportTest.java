package dojo.contacts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Stage3TagsAndExportTest {

    @TempDir
    Path dir;

    private Path book;

    @BeforeEach
    void someContacts() {
        book = dir.resolve("book.txt");
        Cli.run(book, "add", "Ana Lima", "ana@example.com", "+55 11 91234-5678");
        Cli.run(book, "add", "Lima, Bia \"B\"", "bia@example.com");
        Cli.run(book, "add", "Caio", "caio@example.com");
    }

    @Test
    void tagConfirmsAndListShowsSortedTags() {
        var tagged = Cli.run(book, "tag", "ana@example.com", "Work");
        assertEquals(0, tagged.code(), tagged.err());
        assertEquals(List.of("tagged Ana Lima with work"), tagged.lines());
        Cli.run(book, "tag", "ana@example.com", "family");
        Cli.run(book, "tag", "ana@example.com", "work");
        assertEquals(
                "Ana Lima <ana@example.com> +55 11 91234-5678 [family, work]",
                Cli.run(book, "list").lines().get(0));
        assertEquals("Caio <caio@example.com>", Cli.run(book, "list").lines().get(1));
    }

    @Test
    void anInvalidTagOrUnknownEmailFails() {
        var bad = Cli.run(book, "tag", "ana@example.com", "no spaces!");
        assertEquals(1, bad.code());
        assertEquals("invalid tag: no spaces!", bad.err().strip());
        var unknown = Cli.run(book, "tag", "ghost@example.com", "work");
        assertEquals(1, unknown.code());
        assertEquals("no contact with email ghost@example.com", unknown.err().strip());
    }

    @Test
    void listCanKeepOneTag() {
        Cli.run(book, "tag", "caio@example.com", "gym");
        Cli.run(book, "tag", "bia@example.com", "gym");
        Cli.run(book, "tag", "ana@example.com", "work");
        assertEquals(
                List.of("Caio <caio@example.com> [gym]", "Lima, Bia \"B\" <bia@example.com> [gym]"),
                Cli.run(book, "list", "--tag", "gym").lines());
        assertEquals(List.of("no contacts"), Cli.run(book, "list", "--tag", "nobody").lines());
    }

    @Test
    void exportPrintsQuotedCsvInListOrder() {
        Cli.run(book, "tag", "ana@example.com", "work");
        Cli.run(book, "tag", "ana@example.com", "family");
        var result = Cli.run(book, "export");
        assertEquals(0, result.code(), result.err());
        assertEquals(
                List.of(
                        "name,email,phone,tags",
                        "Ana Lima,ana@example.com,+55 11 91234-5678,family;work",
                        "Caio,caio@example.com,,",
                        "\"Lima, Bia \"\"B\"\"\",bia@example.com,,"),
                result.lines());
    }

    @Test
    void tagsAreSavedInTheFile() {
        Cli.run(book, "tag", "caio@example.com", "team-2");
        assertEquals(
                List.of("Caio <caio@example.com> [team-2]"),
                Cli.run(book, "find", "caio").lines());
    }
}
