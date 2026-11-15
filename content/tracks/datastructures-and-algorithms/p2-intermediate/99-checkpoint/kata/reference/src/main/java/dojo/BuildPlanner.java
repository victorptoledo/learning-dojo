package dojo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

/** Orders build tasks, finds cycles, and computes finish times and the critical path. */
public final class BuildPlanner {
    private record Task(String name, int duration, List<String> dependsOn) {}

    /** Everything derived from the tasks, computed once until the next addTask. */
    private record Analysis(Map<String, List<String>> dependents, List<String> order, Map<String, Long> finish) {}

    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private Analysis analysis;

    public void addTask(String name, int duration, List<String> dependsOn) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("a task needs a name");
        if (tasks.containsKey(name)) throw new IllegalArgumentException("duplicate task " + name);
        if (duration < 0) throw new IllegalArgumentException("negative duration for " + name);
        var deps = List.copyOf(new LinkedHashSet<>(dependsOn));
        if (deps.contains(name)) throw new IllegalArgumentException(name + " depends on itself");
        tasks.put(name, new Task(name, duration, deps));
        analysis = null;
    }

    public List<String> order() {
        Analysis a = analyse();
        if (a.order().size() < tasks.size()) throw new IllegalStateException("the tasks have a cycle");
        return a.order();
    }

    public Optional<List<String>> findCycle() {
        analyse(); // checks that every dependency is known
        Map<String, Integer> onPath = new HashMap<>(); // gray tasks and their position in the path
        var done = new java.util.HashSet<String>();
        List<String> path = new ArrayList<>();
        Deque<Integer> nextDep = new ArrayDeque<>(); // per path entry, the next dependency to try
        for (String start : tasks.keySet()) {
            if (done.contains(start)) continue;
            path.add(start);
            onPath.put(start, 0);
            nextDep.push(0);
            while (!path.isEmpty()) {
                String current = path.getLast();
                List<String> deps = tasks.get(current).dependsOn();
                int i = nextDep.pop();
                if (i == deps.size()) {
                    path.removeLast();
                    onPath.remove(current);
                    done.add(current);
                    continue;
                }
                nextDep.push(i + 1);
                String dep = deps.get(i);
                Integer at = onPath.get(dep);
                if (at != null) {
                    List<String> cycle = new ArrayList<>(path.subList(at, path.size()));
                    cycle.add(dep);
                    return Optional.of(cycle);
                }
                if (!done.contains(dep)) {
                    onPath.put(dep, path.size());
                    path.add(dep);
                    nextDep.push(0);
                }
            }
        }
        return Optional.empty();
    }

    public long earliestFinish(String task) {
        requireTask(task);
        order(); // throws on a cycle
        return analyse().finish().get(task);
    }

    public long totalTime() {
        order();
        long best = 0;
        for (long f : analyse().finish().values()) best = Math.max(best, f);
        return best;
    }

    public List<String> criticalPath() {
        order();
        Map<String, Long> finish = analyse().finish();
        String current = null;
        for (String name : tasks.keySet()) {
            if (current == null || better(name, current, finish)) current = name;
        }
        List<String> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            String next = null;
            for (String dep : tasks.get(current).dependsOn()) {
                if (next == null || better(dep, next, finish)) next = dep;
            }
            current = next;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean better(String candidate, String best, Map<String, Long> finish) {
        int c = Long.compare(finish.get(candidate), finish.get(best));
        return c > 0 || (c == 0 && candidate.compareTo(best) < 0);
    }

    public SortedSet<String> affectedBy(String task) {
        requireTask(task);
        Map<String, List<String>> dependents = analyse().dependents();
        SortedSet<String> seen = new TreeSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(task);
        while (!queue.isEmpty()) {
            for (String next : dependents.getOrDefault(queue.poll(), List.of())) {
                if (seen.add(next)) queue.add(next);
            }
        }
        seen.remove(task); // only possible with a cycle
        return seen;
    }

    private void requireTask(String task) {
        if (!tasks.containsKey(task)) throw new IllegalArgumentException("unknown task " + task);
    }

    private Analysis analyse() {
        if (analysis != null) return analysis;
        Map<String, List<String>> dependents = new HashMap<>();
        Map<String, Integer> waiting = new HashMap<>();
        for (Task t : tasks.values()) {
            for (String dep : t.dependsOn()) {
                if (!tasks.containsKey(dep)) throw new IllegalStateException(t.name() + " depends on unknown task " + dep);
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(t.name());
            }
            waiting.put(t.name(), t.dependsOn().size());
        }
        PriorityQueue<String> ready = new PriorityQueue<>();
        waiting.forEach((name, count) -> {
            if (count == 0) ready.add(name);
        });
        List<String> order = new ArrayList<>(tasks.size());
        Map<String, Long> finish = new HashMap<>();
        while (!ready.isEmpty()) {
            String name = ready.poll();
            Task t = tasks.get(name);
            long start = 0;
            for (String dep : t.dependsOn()) start = Math.max(start, finish.get(dep));
            finish.put(name, start + t.duration());
            order.add(name);
            for (String next : dependents.getOrDefault(name, List.of())) {
                if (waiting.merge(next, -1, Integer::sum) == 0) ready.add(next);
            }
        }
        analysis = new Analysis(dependents, List.copyOf(order), finish);
        return analysis;
    }
}
