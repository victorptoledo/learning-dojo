package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.fitness.ClassGraph;
import dojo.fitness.FitnessSuite;
import dojo.fitness.Ratchet;
import dojo.fitness.RatchetResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class SuiteTest {
    private static Set<String> baseline() throws IOException {
        try (InputStream in = SuiteTest.class.getResourceAsStream("/fitness-baseline.txt")) {
            Set<String> lines = new TreeSet<>();
            new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip).filter(line -> !line.isEmpty()).forEach(lines::add);
            return lines;
        }
    }

    @Test
    void theAppAddsNoNewViolations() throws IOException {
        Set<String> current = FitnessSuite.violations(ClassGraph.scan("dojo.app"), ClassGraph.abstractTypes("dojo.app"));
        RatchetResult result = Ratchet.check(baseline(), current);

        assertTrue(result.passes(), "new violations: " + result.newViolations());
        assertEquals(List.of(), result.fixed(), "remove these from the baseline: " + result.fixed());
    }
}
