package dojo.fitness;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** The shop's fitness functions: see the README for the three rules. */
public final class ArchitectureRules {
    private ArchitectureRules() {}

    /** Every violation in a class graph from {@link ClassGraph#scan}, sorted. */
    public static SortedSet<String> violations(Map<String, Set<String>> classGraph) {
        return new TreeSet<>(); // TODO
    }
}
