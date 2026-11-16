package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.HexagonalRules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HexagonalRulesGradingTest {
    @Test
    void aCleanHexagonHasNoViolations() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Loan", Set.of("java.time.LocalDate"),
                "a.domain.Loans", Set.of("a.domain.Loan"),
                "a.application.Borrow", Set.of("a.domain.Loans", "java.time.Clock"),
                "a.adapters.MemLoans", Set.of("a.domain.Loans", "java.lang.System"),
                "a.App", Set.of("a.adapters.MemLoans", "a.application.Borrow"));
        assertEquals(List.of(), HexagonalRules.violations(graph, "a"));
    }

    @Test
    void theDomainUsesNeitherTheApplicationNorTheOuterRing() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Policy", Set.of("a.application.Borrow", "a.adapters.db.MemLoans", "a.App", "a.domain.Loan"));
        assertEquals(List.of(
                        "a.domain.Policy -> a.App",
                        "a.domain.Policy -> a.adapters.db.MemLoans",
                        "a.domain.Policy -> a.application.Borrow"),
                HexagonalRules.violations(graph, "a"));
    }

    @Test
    void theApplicationUsesNoAdapterAndNotTheCompositionRoot() {
        Map<String, Set<String>> graph = Map.of(
                "a.application.Borrow", Set.of("a.adapters.Console", "a.Library", "a.domain.Loan"),
                "a.application.sub.Return", Set.of("a.adapters.MemLoans"));
        assertEquals(List.of(
                        "a.application.Borrow -> a.Library",
                        "a.application.Borrow -> a.adapters.Console",
                        "a.application.sub.Return -> a.adapters.MemLoans"),
                HexagonalRules.violations(graph, "a"));
    }

    @Test
    void theCoreDoesNotTouchSystem() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Policy", Set.of("java.lang.System"),
                "a.application.Borrow", Set.of("java.lang.System", "java.lang.String"),
                "a.App", Set.of("java.lang.System"));
        assertEquals(List.of(
                        "a.application.Borrow -> java.lang.System",
                        "a.domain.Policy -> java.lang.System"),
                HexagonalRules.violations(graph, "a"));
    }

    @Test
    void onlyClassesDirectlyInTheRootAreTheCompositionRoot() {
        Map<String, Set<String>> graph = Map.of(
                "a.application.Borrow", Set.of("a.domain.Loan", "a.domainextra.Thing", "b.Other"),
                "a.domain.Loan", Set.of("a.domain.sub.Money"));
        assertEquals(List.of(), HexagonalRules.violations(graph, "a"));
    }
}
