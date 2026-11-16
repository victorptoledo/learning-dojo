package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.fitness.Cycles;
import dojo.fitness.Ratchet;
import dojo.fitness.RatchetResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CyclesRatchetGradingTest {
    @Test
    void aLineHasNoCycle() {
        assertEquals(List.of(), Cycles.find(Map.of("a", Set.of("b"), "b", Set.of("c"), "c", Set.of())));
    }

    @Test
    void aLongCycleIsOneGroup() {
        Map<String, Set<String>> graph = Map.of(
                "z", Set.of("y"), "y", Set.of("x"), "x", Set.of("z", "w"), "w", Set.of());
        assertEquals(List.of(List.of("x", "y", "z")), Cycles.find(graph));
    }

    @Test
    void twoSeparateCyclesAreSortedByTheirFirstName() {
        Map<String, Set<String>> graph = Map.of(
                "m", Set.of("n"), "n", Set.of("m", "b"),
                "b", Set.of("a"), "a", Set.of("b"),
                "q", Set.of("m"));
        assertEquals(List.of(List.of("a", "b"), List.of("m", "n")), Cycles.find(graph));
    }

    @Test
    void aTargetMissingFromTheKeysIsNotAProblem() {
        assertEquals(List.of(), Cycles.find(Map.of("a", Set.of("elsewhere"))));
    }

    @Test
    void theRatchetFailsOnlyOnNewViolations() {
        RatchetResult same = Ratchet.check(Set.of("x", "y"), Set.of("y", "x"));
        assertTrue(same.passes());
        assertEquals(List.of(), same.newViolations());
        assertEquals(List.of(), same.fixed());

        RatchetResult better = Ratchet.check(Set.of("x", "y", "a"), Set.of("y"));
        assertTrue(better.passes());
        assertEquals(List.of("a", "x"), better.fixed());

        RatchetResult worse = Ratchet.check(Set.of("x"), Set.of("x", "q", "b"));
        assertFalse(worse.passes());
        assertEquals(List.of("b", "q"), worse.newViolations());
    }
}
