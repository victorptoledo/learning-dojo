package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.FitnessSuite;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SuiteGradingTest {
    @Test
    void theSuiteReportsCyclesAndPackagesFarFromTheMainSequence() {
        Map<String, Set<String>> graph = Map.of(
                "s.a.One", Set.of("s.b.Two"),
                "s.b.Two", Set.of("s.a.One"),
                "s.c.Base", Set.of(),
                "s.c.Rule", Set.of(),
                "s.d.User", Set.of("s.c.Base", "s.c.Rule"));
        assertEquals(List.of(
                        "cycle: s.a, s.b",
                        "main sequence: s.c D=1.00"),
                List.copyOf(FitnessSuite.violations(graph, Set.of())));
    }

    @Test
    void onlyDistancesAboveTheLimitAreReported() {
        Map<String, Set<String>> graph = Map.of(
                "t.a.A1", Set.of(), "t.a.C1", Set.of(), "t.a.C2", Set.of(), "t.a.C3", Set.of(),
                "t.b.A1", Set.of(), "t.b.C1", Set.of());
        assertEquals(List.of("main sequence: t.a D=0.75"),
                List.copyOf(FitnessSuite.violations(graph, Set.of("t.a.A1", "t.b.A1"))));
    }

    @Test
    void theRealAppMatchesItsBaseline() {
        assertEquals(List.of("cycle: dojo.app.store, dojo.app.web"),
                List.copyOf(FitnessSuite.violations(GradingGraph.scan("dojo.app"), GradingGraph.abstractTypes("dojo.app"))));
    }
}
