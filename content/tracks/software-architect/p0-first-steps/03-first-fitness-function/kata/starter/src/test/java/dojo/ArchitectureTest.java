package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import dojo.fitness.Rules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final Map<String, Set<String>> SHOP = ClassGraph.scan("dojo.shop");

    @Test
    void theDomainUsesNothingFromApplicationOrInfra() {
        assertEquals(List.of(), Rules.violations(SHOP, "dojo.shop.domain",
                Set.of("dojo.shop.application", "dojo.shop.infra")));
    }

    @Test
    void aRuleFindsUsesOfASubpackage() {
        Map<String, Set<String>> graph = Map.of(
                "a.domain.Order", Set.of("a.infra.db.Row", "java.util.List"),
                "a.infra.db.Row", Set.of());
        assertEquals(List.of("a.domain.Order -> a.infra.db.Row"),
                Rules.violations(graph, "a.domain", Set.of("a.infra")));
    }
}
