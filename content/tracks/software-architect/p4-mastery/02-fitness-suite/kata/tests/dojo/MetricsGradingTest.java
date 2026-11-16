package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.Metrics;
import dojo.fitness.PackageMetrics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MetricsGradingTest {
    private static void assertMetrics(PackageMetrics actual, int ca, int ce, double i, double a, double d) {
        String name = actual.pkg();
        assertEquals(ca, actual.afferent(), name + " Ca");
        assertEquals(ce, actual.efferent(), name + " Ce");
        assertEquals(i, actual.instability(), 1e-9, name + " I");
        assertEquals(a, actual.abstractness(), 1e-9, name + " A");
        assertEquals(d, actual.distance(), 1e-9, name + " D");
    }

    @Test
    void martinsMetricsForThreePackages() {
        Map<String, Set<String>> graph = Map.of(
                "p.api.Port", Set.of("java.lang.String"),
                "p.api.Value", Set.of(),
                "p.core.Service", Set.of("p.api.Port", "p.api.Value", "java.util.List"),
                "p.core.Helper", Set.of("p.api.Value"),
                "p.web.Controller", Set.of("p.core.Service", "p.api.Port"));
        Map<String, PackageMetrics> metrics = Metrics.of(graph, Set.of("p.api.Port"));

        assertEquals(List.of("p.api", "p.core", "p.web"), List.copyOf(metrics.keySet()));
        assertMetrics(metrics.get("p.api"), 3, 0, 0.0, 0.5, 0.5);
        assertMetrics(metrics.get("p.core"), 1, 2, 2.0 / 3, 0.0, 1.0 / 3);
        assertMetrics(metrics.get("p.web"), 0, 2, 1.0, 0.0, 0.0);
    }

    @Test
    void anIsolatedPackageHasZeroInstability() {
        Map<String, Set<String>> graph = Map.of("q.alone.Thing", Set.of("java.util.Map"));
        assertMetrics(Metrics.of(graph, Set.of()).get("q.alone"), 0, 0, 0.0, 0.0, 1.0);
    }

    @Test
    void eachClassCountsOnceHoweverManyUsesItHas() {
        Map<String, Set<String>> graph = Map.of(
                "r.a.One", Set.of("r.b.X", "r.b.Y"),
                "r.a.Two", Set.of("r.b.X"),
                "r.b.X", Set.of(),
                "r.b.Y", Set.of());
        Map<String, PackageMetrics> metrics = Metrics.of(graph, Set.of("r.b.X", "r.b.Y"));
        assertMetrics(metrics.get("r.a"), 0, 2, 1.0, 0.0, 0.0);
        assertMetrics(metrics.get("r.b"), 2, 0, 0.0, 1.0, 0.0);
    }
}
