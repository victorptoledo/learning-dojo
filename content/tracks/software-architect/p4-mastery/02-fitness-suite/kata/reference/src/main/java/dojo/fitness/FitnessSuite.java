package dojo.fitness;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** The suite the build runs. */
public final class FitnessSuite {
    private FitnessSuite() {}

    public static Set<String> violations(Map<String, Set<String>> classGraph, Set<String> abstractTypes) {
        Set<String> found = new TreeSet<>();
        for (List<String> cycle : Cycles.find(ClassGraph.packageDependencies(classGraph))) {
            found.add("cycle: " + String.join(", ", cycle));
        }
        Metrics.of(classGraph, abstractTypes).values().stream()
                .filter(metrics -> metrics.distance() > 0.7)
                .forEach(metrics -> found.add(String.format(Locale.ROOT, "main sequence: %s D=%.2f",
                        metrics.pkg(), metrics.distance())));
        return Collections.unmodifiableSet(found);
    }
}
