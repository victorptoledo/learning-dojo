package dojo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** INC-1's root cause, checked in the sources: no binary floating point, no time without a zone. */
class PrecisionGradingTest {
    static final Path SOURCES = Path.of("src", "main", "java");
    static final Pattern FORBIDDEN = Pattern.compile("\\b(double|float|Double|Float|LocalDateTime)\\b");

    /** The source with comments, string literals and char literals blanked. */
    static String code(String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("//[^\\n]*", " ")
                .replaceAll("(?s)\"\"\".*?\"\"\"", "\"\"")
                .replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(\\\\.|[^'\\\\])*'", "' '");
    }

    @Test
    void money_and_time_use_exact_types() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = FORBIDDEN.matcher(code(Files.readString(file)));
                while (matcher.find()) {
                    found.add(SOURCES.relativize(file) + ": " + matcher.group());
                }
            }
        } catch (IOException e) {
            fail("cannot read the sources: " + e.getMessage());
        }
        assertTrue(found.isEmpty(), "money is a long in cents and instants are Instant (UTC); found " + found);
    }
}
