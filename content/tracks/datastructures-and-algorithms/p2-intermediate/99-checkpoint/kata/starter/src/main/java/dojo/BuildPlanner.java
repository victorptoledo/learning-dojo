package dojo;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

/** Orders build tasks, finds cycles, and computes finish times and the critical path. */
public final class BuildPlanner {

    /** Adds a task; dependencies may be added later. */
    public void addTask(String name, int duration, List<String> dependsOn) {
        throw new UnsupportedOperationException("todo addTask " + name);
    }

    /** Every task after its dependencies; among ready tasks, the alphabetically smallest first. */
    public List<String> order() {
        throw new UnsupportedOperationException("todo order");
    }

    /** One cycle [a, b, ..., a] where each task depends on the next, or empty. */
    public Optional<List<String>> findCycle() {
        throw new UnsupportedOperationException("todo findCycle");
    }

    /** The task's duration plus the latest finish among its dependencies. */
    public long earliestFinish(String task) {
        throw new UnsupportedOperationException("todo earliestFinish " + task);
    }

    /** The latest finish of any task, 0 with no tasks. */
    public long totalTime() {
        throw new UnsupportedOperationException("todo totalTime");
    }

    /** The chain of tasks that sets the total time, first task first. */
    public List<String> criticalPath() {
        throw new UnsupportedOperationException("todo criticalPath");
    }

    /** Every task that depends on this one, directly or not, sorted. */
    public SortedSet<String> affectedBy(String task) {
        throw new UnsupportedOperationException("todo affectedBy " + task);
    }
}
