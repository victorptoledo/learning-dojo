package dojo.fitness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The dependency rule of a hexagon, as code over the graph {@link ClassGraph#scan} returns. */
public final class HexagonalRules {
    private HexagonalRules() {}

    /** Every forbidden use under {@code root}, as sorted {@code "from -> to"} lines. */
    public static List<String> violations(Map<String, Set<String>> graph, String root) {
        List<String> found = new ArrayList<>();
        graph.forEach((from, uses) -> {
            boolean inDomain = isIn(from, root + ".domain");
            boolean inApplication = isIn(from, root + ".application");
            for (String to : uses) {
                boolean forbidden = inDomain && (isIn(to, root + ".application") || outerRing(to, root))
                        || inApplication && outerRing(to, root)
                        || (inDomain || inApplication) && to.equals("java.lang.System");
                if (forbidden) {
                    found.add(from + " -> " + to);
                }
            }
        });
        return found.stream().distinct().sorted().toList();
    }

    private static boolean outerRing(String className, String root) {
        return isIn(className, root + ".adapters") || ClassGraph.packageOf(className).equals(root);
    }

    private static boolean isIn(String className, String pkg) {
        return className.startsWith(pkg + ".");
    }
}
