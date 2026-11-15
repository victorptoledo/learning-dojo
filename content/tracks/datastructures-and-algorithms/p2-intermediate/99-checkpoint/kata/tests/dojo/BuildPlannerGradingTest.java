package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BuildPlannerGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(5);

    private record Spec(String name, int duration, List<String> deps) {}

    private static BuildPlanner plan(List<Spec> specs) {
        var plan = new BuildPlanner();
        for (Spec s : specs) plan.addTask(s.name(), s.duration(), s.deps());
        return plan;
    }

    /** A random graph; acyclic when every task only depends on tasks created before it. */
    private static List<Spec> randomSpecs(Random random, int n, boolean acyclic) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < n; i++) names.add("t" + (char) ('a' + i));
        Collections.shuffle(names, random); // creation order differs from name order
        List<Spec> specs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Set<String> deps = new LinkedHashSet<>();
            int candidates = acyclic ? i : n;
            for (int j = 0; j < candidates; j++) {
                if (j != i && random.nextInt(3) == 0) deps.add(names.get(j));
            }
            specs.add(new Spec(names.get(i), random.nextInt(10), List.copyOf(deps)));
        }
        Collections.shuffle(specs, random); // dependencies may be added later
        return specs;
    }

    // ---- slow references ----

    private static Map<String, Spec> byName(List<Spec> specs) {
        Map<String, Spec> map = new HashMap<>();
        for (Spec s : specs) map.put(s.name(), s);
        return map;
    }

    private static List<String> slowOrder(List<Spec> specs) {
        var placed = new LinkedHashSet<String>();
        while (placed.size() < specs.size()) {
            String next = null;
            for (Spec s : specs) {
                if (!placed.contains(s.name()) && placed.containsAll(s.deps()) && (next == null || s.name().compareTo(next) < 0)) {
                    next = s.name();
                }
            }
            if (next == null) return null; // a cycle
            placed.add(next);
        }
        return List.copyOf(placed);
    }

    private static long slowFinish(Map<String, Spec> specs, String name) {
        long start = 0;
        for (String dep : specs.get(name).deps()) start = Math.max(start, slowFinish(specs, dep));
        return start + specs.get(name).duration();
    }

    private static List<String> slowCritical(List<Spec> specs) {
        var map = byName(specs);
        String current = null;
        for (Spec s : specs) {
            if (current == null || betterSlow(map, s.name(), current)) current = s.name();
        }
        var path = new ArrayList<String>();
        while (current != null) {
            path.add(current);
            String next = null;
            for (String dep : map.get(current).deps()) {
                if (next == null || betterSlow(map, dep, next)) next = dep;
            }
            current = next;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean betterSlow(Map<String, Spec> map, String a, String b) {
        int c = Long.compare(slowFinish(map, a), slowFinish(map, b));
        return c > 0 || (c == 0 && a.compareTo(b) < 0);
    }

    private static Set<String> slowAffected(List<Spec> specs, String task) {
        Set<String> found = new TreeSet<>();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Spec s : specs) {
                if (!found.contains(s.name()) && (s.deps().contains(task) || s.deps().stream().anyMatch(found::contains))) {
                    grew |= found.add(s.name());
                }
            }
        }
        found.remove(task);
        return found;
    }

    private static void assertValidCycle(List<Spec> specs, List<String> cycle) {
        var map = byName(specs);
        assertTrue(cycle.size() >= 3, "a cycle needs at least two tasks: " + cycle);
        assertEquals(cycle.getFirst(), cycle.getLast(), "starts and ends with the same task: " + cycle);
        for (int i = 0; i + 1 < cycle.size(); i++) {
            assertTrue(map.get(cycle.get(i)).deps().contains(cycle.get(i + 1)), cycle.get(i) + " must depend on " + cycle.get(i + 1));
        }
    }

    // ---- small random graphs ----

    @Test
    void acyclic_graphs_match_the_slow_references() {
        var random = new Random(31);
        for (int round = 0; round < 300; round++) {
            var specs = randomSpecs(random, 1 + random.nextInt(9), true);
            var plan = plan(specs);
            var map = byName(specs);
            assertEquals(slowOrder(specs), plan.order(), specs.toString());
            assertEquals(Optional.empty(), plan.findCycle());
            long total = 0;
            for (Spec s : specs) {
                assertEquals(slowFinish(map, s.name()), plan.earliestFinish(s.name()), s.name());
                total = Math.max(total, slowFinish(map, s.name()));
                assertEquals(slowAffected(specs, s.name()), plan.affectedBy(s.name()), "affected by " + s.name());
            }
            assertEquals(total, plan.totalTime());
            assertEquals(slowCritical(specs), plan.criticalPath(), specs.toString());
        }
    }

    @Test
    void cycles_are_found_and_block_ordering() {
        var random = new Random(32);
        int withCycle = 0;
        for (int round = 0; round < 300; round++) {
            var specs = randomSpecs(random, 2 + random.nextInt(7), false);
            var plan = plan(specs);
            boolean cyclic = slowOrder(specs) == null;
            Optional<List<String>> cycle = plan.findCycle();
            assertEquals(cyclic, cycle.isPresent(), specs.toString());
            if (cyclic) {
                withCycle++;
                assertValidCycle(specs, cycle.get());
                assertThrows(IllegalStateException.class, plan::order);
                assertThrows(IllegalStateException.class, plan::totalTime);
                assertThrows(IllegalStateException.class, plan::criticalPath);
                assertThrows(IllegalStateException.class, () -> plan.earliestFinish(specs.getFirst().name()));
            }
        }
        assertTrue(withCycle > 50, "the generator should produce cycles");
    }

    @Test
    void bad_input_and_unknown_tasks() {
        var plan = new BuildPlanner();
        assertThrows(IllegalArgumentException.class, () -> plan.addTask(" ", 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> plan.addTask(null, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> plan.addTask("a", -1, List.of()));
        assertThrows(IllegalArgumentException.class, () -> plan.addTask("a", 1, List.of("a")));
        assertEquals(List.of(), plan.order());
        assertEquals(0, plan.totalTime());
        assertEquals(List.of(), plan.criticalPath());
        plan.addTask("a", 1, List.of("b", "b"));
        assertThrows(IllegalArgumentException.class, () -> plan.addTask("a", 1, List.of()));
        assertThrows(IllegalStateException.class, plan::order, "b is not known yet");
        assertThrows(IllegalStateException.class, plan::findCycle);
        plan.addTask("b", 2, List.of());
        assertEquals(List.of("b", "a"), plan.order(), "the answer changes after addTask");
        assertEquals(3, plan.earliestFinish("a"));
        assertThrows(IllegalArgumentException.class, () -> plan.earliestFinish("zzz"));
        assertThrows(IllegalArgumentException.class, () -> plan.affectedBy("zzz"));
    }

    // ---- large builds ----

    private static String name(int i) {
        return String.format("t%06d", i);
    }

    @Test
    void a_long_chain_added_backwards() {
        int n = 200_000;
        var plan = new BuildPlanner();
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int i = n - 1; i >= 0; i--) plan.addTask(name(i), 1, i == 0 ? List.of() : List.of(name(i - 1)));
            List<String> order = plan.order();
            assertEquals(n, order.size());
            assertEquals(name(0), order.getFirst());
            assertEquals(name(n - 1), order.getLast());
            assertEquals(Optional.empty(), plan.findCycle());
            assertEquals(n, plan.earliestFinish(name(n - 1)));
            assertEquals(n, plan.totalTime());
            assertEquals(n, plan.criticalPath().size());
            assertEquals(n - 1, plan.affectedBy(name(0)).size());
        });
    }

    @Test
    void a_long_cycle_is_found_without_overflowing_the_stack() {
        int n = 200_000;
        var plan = new BuildPlanner();
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int i = 0; i < n; i++) plan.addTask(name(i), 1, List.of(name((i + 1) % n)));
            var cycle = plan.findCycle();
            assertTrue(cycle.isPresent());
            assertEquals(n + 1, cycle.get().size());
            assertEquals(cycle.get().getFirst(), cycle.get().getLast());
            assertThrows(IllegalStateException.class, plan::order);
        });
    }

    @Test
    void a_ladder_needs_memoised_finish_times() {
        int n = 100_000;
        var plan = new BuildPlanner();
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int i = 0; i < n; i++) {
                List<String> deps = new ArrayList<>();
                if (i >= 1) deps.add(name(i - 1));
                if (i >= 2) deps.add(name(i - 2));
                plan.addTask(name(i), i % 3, deps);
            }
            long expected = 0;
            for (int i = 0; i < n; i++) expected += i % 3;
            assertEquals(expected, plan.earliestFinish(name(n - 1)));
            assertEquals(expected, plan.totalTime());
        });
    }

    @Test
    void a_wide_build_is_ordered_quickly() {
        int n = 200_000;
        var random = new Random(33);
        var plan = new BuildPlanner();
        int[] parent = new int[n];
        assertTimeoutPreemptively(LIMIT, () -> {
            plan.addTask(name(0), 5, List.of());
            for (int i = 1; i < n; i++) {
                parent[i] = random.nextInt(i);
                plan.addTask(name(i), 5, List.of(name(parent[i])));
            }
            List<String> order = plan.order();
            Map<String, Integer> position = new HashMap<>();
            for (int i = 0; i < order.size(); i++) position.put(order.get(i), i);
            for (int i = 1; i < n; i += 997) {
                assertTrue(position.get(name(parent[i])) < position.get(name(i)));
            }
            assertFalse(plan.affectedBy(name(0)).isEmpty());
        });
        var distinct = new HashSet<>(plan.order());
        assertEquals(n, distinct.size());
    }
}
