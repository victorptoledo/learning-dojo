package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ArchitectureRules;
import dojo.fitness.ClassGraph;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** The ratchet: the build fails on a new violation and asks you to drop the fixed ones. */
class SuiteTest {
    private static Set<String> baseline() throws IOException {
        try (InputStream in = SuiteTest.class.getResourceAsStream("/fitness-baseline.txt")) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new TreeSet<>(text.lines().map(String::strip).filter(line -> !line.isEmpty()).toList());
        }
    }

    @Test
    void noNewViolations() throws IOException {
        SortedSet<String> current = ArchitectureRules.violations(ClassGraph.scan("dojo.shop"));
        Set<String> added = new TreeSet<>(current);
        added.removeAll(baseline());
        assertEquals(Set.of(), added, "new violations: fix them, do not add them to the baseline");
    }

    @Test
    void theBaselineHasNoFixedViolations() throws IOException {
        Set<String> fixed = new TreeSet<>(baseline());
        fixed.removeAll(ArchitectureRules.violations(ClassGraph.scan("dojo.shop")));
        assertEquals(Set.of(), fixed, "fixed violations: remove them from the baseline so they cannot come back");
    }
}
