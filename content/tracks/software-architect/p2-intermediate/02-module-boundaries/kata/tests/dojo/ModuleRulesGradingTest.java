package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ModuleRules;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModuleRulesGradingTest {
    private static final Set<String> MODULES = Set.of("a", "b");

    @Test
    void usesOfAnotherModulesApiAreAllowed() {
        Map<String, Set<String>> graph = Map.of(
                "r.a.internal.Service", Set.of("r.b.api.Port", "r.b.api.model.View", "r.a.internal.Table"),
                "r.b.internal.Impl", Set.of("r.b.api.Port", "r.b.internal.Table"));
        assertEquals(List.of(), ModuleRules.violations(graph, "r", MODULES));
    }

    @Test
    void usesOfAnotherModulesInternalsAreViolations() {
        Map<String, Set<String>> graph = Map.of(
                "r.a.internal.Service", Set.of("r.b.internal.Table", "r.b.Facade"),
                "r.b.api.Port", Set.of("r.a.internal.Thing"));
        assertEquals(List.of(
                        "r.a.internal.Service -> r.b.Facade",
                        "r.a.internal.Service -> r.b.internal.Table",
                        "r.b.api.Port -> r.a.internal.Thing"),
                ModuleRules.violations(graph, "r", MODULES));
    }

    @Test
    void anApiMustNotExposeItsOwnInternals() {
        Map<String, Set<String>> graph = Map.of(
                "r.a.api.View", Set.of("r.a.internal.Row", "r.a.api.Other"),
                "r.a.internal.Row", Set.of("r.a.api.View"));
        assertEquals(List.of("r.a.api.View -> r.a.internal.Row"), ModuleRules.violations(graph, "r", MODULES));
    }

    @Test
    void classesOutsideTheModulesAreNotChecked() {
        Map<String, Set<String>> graph = Map.of(
                "r.App", Set.of("r.a.internal.Service", "r.b.internal.Impl"),
                "r.a.internal.Service", Set.of("r.App", "java.util.List", "r.shared.Money"),
                "r.shared.Money", Set.of("r.b.internal.Impl"));
        assertEquals(List.of(), ModuleRules.violations(graph, "r", MODULES));
    }

    @Test
    void modulesWithSimilarNamesAreDifferentModules() {
        Map<String, Set<String>> graph = Map.of(
                "r.a.internal.Service", Set.of("r.ab.internal.Thing", "r.b.apix.Thing"));
        assertEquals(List.of("r.a.internal.Service -> r.ab.internal.Thing", "r.a.internal.Service -> r.b.apix.Thing"),
                ModuleRules.violations(graph, "r", Set.of("a", "b", "ab")));
    }
}
