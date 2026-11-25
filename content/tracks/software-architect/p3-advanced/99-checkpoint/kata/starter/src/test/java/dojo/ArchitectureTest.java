package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final Map<String, Set<String>> TRAVEL = ClassGraph.scan("dojo.travel");

    private static List<String> uses(String from, Predicate<String> forbidden) {
        return TRAVEL.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(from))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden).map(to -> entry.getKey() + " -> " + to))
                .toList();
    }

    @Test
    void theDomainKnowsNothingOutsideItself() {
        assertEquals(List.of(), uses("dojo.travel.domain.",
                to -> to.startsWith("dojo.travel.") && !to.startsWith("dojo.travel.domain.")));
    }

    @Test
    void theSagaDoesNotKnowTheAdapters() {
        assertEquals(List.of(), uses("dojo.travel.application.", to -> to.startsWith("dojo.travel.adapters.")));
    }
}
