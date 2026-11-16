package dojo.fitness;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Package cycles: groups of packages that all reach each other. */
public final class Cycles {
    private Cycles() {}

    public static List<List<String>> find(Map<String, Set<String>> packageGraph) {
        Map<String, Set<String>> reach = new HashMap<>();
        for (String start : packageGraph.keySet()) {
            reach.put(start, reachable(packageGraph, start));
        }
        Set<String> grouped = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();
        for (String pkg : new TreeSet<>(packageGraph.keySet())) {
            if (grouped.contains(pkg)) {
                continue;
            }
            TreeSet<String> group = new TreeSet<>();
            group.add(pkg);
            for (String other : reach.get(pkg)) {
                if (reach.containsKey(other) && reach.get(other).contains(pkg)) {
                    group.add(other);
                }
            }
            grouped.addAll(group);
            if (group.size() > 1) {
                cycles.add(List.copyOf(group));
            }
        }
        cycles.sort(Comparator.comparing(cycle -> cycle.get(0)));
        return List.copyOf(cycles);
    }

    private static Set<String> reachable(Map<String, Set<String>> graph, String start) {
        Set<String> seen = new HashSet<>();
        Deque<String> todo = new ArrayDeque<>(graph.getOrDefault(start, Set.of()));
        while (!todo.isEmpty()) {
            String next = todo.pop();
            if (seen.add(next)) {
                todo.addAll(graph.getOrDefault(next, Set.of()));
            }
        }
        return seen;
    }
}
