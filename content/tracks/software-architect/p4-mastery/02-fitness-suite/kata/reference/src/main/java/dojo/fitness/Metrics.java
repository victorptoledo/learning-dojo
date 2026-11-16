package dojo.fitness;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Robert C. Martin's package metrics. */
public final class Metrics {
    private Metrics() {}

    public static Map<String, PackageMetrics> of(Map<String, Set<String>> classGraph, Set<String> abstractTypes) {
        Map<String, PackageMetrics> result = new TreeMap<>();
        Set<String> packages = new HashSet<>();
        classGraph.keySet().forEach(name -> packages.add(ClassGraph.packageOf(name)));
        for (String pkg : packages) {
            Set<String> outsideUsers = new HashSet<>();
            Set<String> outsideUsed = new HashSet<>();
            int classes = 0;
            int abstracts = 0;
            for (Map.Entry<String, Set<String>> entry : classGraph.entrySet()) {
                String from = entry.getKey();
                boolean inside = ClassGraph.packageOf(from).equals(pkg);
                if (inside) {
                    classes++;
                    if (abstractTypes.contains(from)) {
                        abstracts++;
                    }
                }
                for (String to : entry.getValue()) {
                    if (!classGraph.containsKey(to)) {
                        continue;
                    }
                    boolean targetInside = ClassGraph.packageOf(to).equals(pkg);
                    if (inside && !targetInside) {
                        outsideUsed.add(to);
                    } else if (!inside && targetInside) {
                        outsideUsers.add(from);
                    }
                }
            }
            int ca = outsideUsers.size();
            int ce = outsideUsed.size();
            double instability = ca + ce == 0 ? 0.0 : (double) ce / (ca + ce);
            double abstractness = (double) abstracts / classes;
            double distance = Math.abs(abstractness + instability - 1);
            result.put(pkg, new PackageMetrics(pkg, ca, ce, instability, abstractness, distance));
        }
        return Collections.unmodifiableMap(result);
    }
}
