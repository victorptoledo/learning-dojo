package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import dojo.fitness.HexagonalRules;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    @Test
    void theLibraryKeepsTheDependencyRule() {
        assertEquals(List.of(), HexagonalRules.violations(ClassGraph.scan("dojo.library"), "dojo.library"));
    }
}
