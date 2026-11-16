package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import dojo.fitness.ModuleRules;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    @Test
    void modulesTalkOnlyThroughTheirApis() {
        assertEquals(List.of(), ModuleRules.violations(ClassGraph.scan("dojo.store"), "dojo.store",
                Set.of("catalog", "orders", "shipping")));
    }
}
