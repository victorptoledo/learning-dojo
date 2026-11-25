package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DecisionRecordGradingTest {
    private static final Path ADRS = Path.of("docs", "adr");
    private static final List<String> SECTIONS = List.of("Status", "Context", "Decision", "Consequences");

    private static List<Path> records() {
        try (Stream<Path> files = Files.list(ADRS)) {
            return files.filter(file -> file.getFileName().toString().matches("\\d{4}-[a-z0-9-]+\\.md")).sorted().toList();
        } catch (IOException e) {
            return fail("cannot list " + ADRS + ": " + e.getMessage());
        }
    }

    /** The text under each `## ` heading. */
    private static Map<String, String> sections(Path file) throws IOException {
        Map<String, String> found = new LinkedHashMap<>();
        String current = null;
        StringBuilder text = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (line.startsWith("## ")) {
                if (current != null) {
                    found.put(current, text.toString().strip());
                }
                current = line.substring(3).strip();
                text.setLength(0);
            } else if (current != null) {
                text.append(line).append('\n');
            }
        }
        if (current != null) {
            found.put(current, text.toString().strip());
        }
        return found;
    }

    @Test
    void theMigrationIsRecordedAsADecision() throws IOException {
        List<Path> records = records();
        assertFalse(records.isEmpty(), "add a decision record such as docs/adr/0001-strangle-the-pricing-engine.md");
        Path record = records.getFirst();
        String text = Files.readString(record);
        assertTrue(text.lines().anyMatch(line -> line.startsWith("# ")), record + " needs a title line starting with '# '");
        Map<String, String> sections = sections(record);
        assertEquals(SECTIONS, sections.keySet().stream().filter(SECTIONS::contains).toList(),
                record + " needs the sections Status, Context, Decision and Consequences, in that order");
        String status = sections.get("Status");
        assertTrue(status.startsWith("Accepted") || status.startsWith("Proposed"),
                "the status should start with Accepted or Proposed, not: " + status);
        for (String section : List.of("Context", "Decision", "Consequences")) {
            long words = sections.get(section).split("\\s+").length;
            assertTrue(words >= 15, "the " + section + " section of " + record + " says too little (" + words + " words)");
        }
        String body = text.toLowerCase(java.util.Locale.ROOT);
        assertTrue(body.contains("shadow") || body.contains("parallel run"),
                "the decision should say how the new pricing is checked against the old one");
    }
}
