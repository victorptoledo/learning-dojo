package dojo.fitness;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Architectural rules written as code, over the graph that {@link ClassGraph#scan} returns. */
public final class Rules {
    private Rules() {}

    /**
     * The dependencies that break "classes in {@code fromPackage} use nothing in the
     * {@code forbiddenPackages}", as {@code "from -> to"} lines, sorted and without duplicates.
     * A class is in a package when its name starts with the package name and a dot.
     */
    public static List<String> violations(Map<String, Set<String>> graph, String fromPackage,
                                          Set<String> forbiddenPackages) {
        return graph.entrySet().stream()
                .filter(entry -> isIn(entry.getKey(), fromPackage))
                .flatMap(entry -> entry.getValue().stream()
                        .filter(target -> forbiddenPackages.stream().anyMatch(pkg -> isIn(target, pkg)))
                        .map(target -> entry.getKey() + " -> " + target))
                .distinct()
                .sorted()
                .toList();
    }

    private static boolean isIn(String className, String pkg) {
        return className.startsWith(pkg + ".");
    }
}
