package dojo.fitness;

import java.util.Map;
import java.util.Set;

/** Robert C. Martin's package metrics. */
public final class Metrics {
    private Metrics() {}

    public static Map<String, PackageMetrics> of(Map<String, Set<String>> classGraph, Set<String> abstractTypes) {
        throw new UnsupportedOperationException("todo: compute the package metrics");
    }
}
