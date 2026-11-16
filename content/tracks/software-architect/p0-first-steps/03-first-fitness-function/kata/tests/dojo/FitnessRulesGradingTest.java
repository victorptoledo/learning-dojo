package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.Rules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FitnessRulesGradingTest {
    @Test
    void aCleanGraphHasNoViolations() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Order", Set.of("a.domain.Line", "java.util.List"),
                "a.infra.Table", Set.of("a.domain.Order"));
        assertEquals(List.of(), Rules.violations(graph, "a.domain", Set.of("a.infra")));
    }

    @Test
    void everyForbiddenUseIsListedSorted() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Price", Set.of("a.infra.Fx", "a.app.Checkout"),
                "a.domain.Order", Set.of("a.infra.Row", "a.domain.Line"));
        assertEquals(List.of(
                        "a.domain.Order -> a.infra.Row",
                        "a.domain.Price -> a.app.Checkout",
                        "a.domain.Price -> a.infra.Fx"),
                Rules.violations(graph, "a.domain", Set.of("a.infra", "a.app")));
    }

    @Test
    void subpackagesOfBothSidesCount() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.pricing.Tax", Set.of("a.infra.http.Client"));
        assertEquals(List.of("a.domain.pricing.Tax -> a.infra.http.Client"),
                Rules.violations(graph, "a.domain", Set.of("a.infra")));
    }

    @Test
    void aPackageWithALongerNameIsNotASubpackage() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Order", Set.of("a.infrastructure.Row"),
                "a.domainx.Thing", Set.of("a.infra.Row"));
        assertEquals(List.of(), Rules.violations(graph, "a.domain", Set.of("a.infra")));
    }

    @Test
    void classesOutsideTheRuledPackageMayUseAnything() {
        Map<String, Set<String>> graph = Map.of(
                "a.App", Set.of("a.infra.Row", "a.domain.Order"),
                "a.infra.Row", Set.of("a.domain.Order"));
        assertEquals(List.of(), Rules.violations(graph, "a.domain", Set.of("a.infra")));
    }
}
