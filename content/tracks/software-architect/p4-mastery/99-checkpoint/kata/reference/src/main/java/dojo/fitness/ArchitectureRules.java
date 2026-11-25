package dojo.fitness;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

/** The shop's fitness functions: see the README for the three rules. */
public final class ArchitectureRules {
    /** The composition root: the classes of the package {@code dojo.shop} itself. */
    private static final Predicate<String> ROOT = name -> name.lastIndexOf('.') == "dojo.shop".length() && name.startsWith("dojo.shop.");
    private static final String PRICING = "dojo.shop.pricing.";

    private ArchitectureRules() {}

    /** Every violation in a class graph from {@link ClassGraph#scan}, sorted. */
    public static SortedSet<String> violations(Map<String, Set<String>> classGraph) {
        SortedSet<String> found = new TreeSet<>();
        check(classGraph, "legacy",
                from -> !ROOT.test(from) && !from.startsWith(PRICING + "legacy."),
                to -> to.startsWith("dojo.legacy."), found);
        check(classGraph, "modern",
                from -> from.startsWith(PRICING + "modern."),
                to -> to.startsWith(PRICING + "legacy.") || to.startsWith(PRICING + "migration."), found);
        check(classGraph, "api",
                from -> !ROOT.test(from) && !from.startsWith(PRICING),
                to -> to.startsWith(PRICING) && !to.startsWith(PRICING + "api."), found);
        return found;
    }

    private static void check(Map<String, Set<String>> graph, String rule, Predicate<String> from,
            Predicate<String> forbidden, SortedSet<String> into) {
        graph.forEach((name, uses) -> {
            if (from.test(name)) {
                uses.stream().filter(forbidden).forEach(to -> into.add(rule + ": " + name + " -> " + to));
            }
        });
    }
}
