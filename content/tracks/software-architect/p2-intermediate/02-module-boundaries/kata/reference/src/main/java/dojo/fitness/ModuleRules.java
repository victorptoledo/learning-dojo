package dojo.fitness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The boundary rule of a modular monolith, over the graph {@link ClassGraph#scan} returns. */
public final class ModuleRules {
    private ModuleRules() {}

    /** Every use that crosses a module boundary other than through an api, sorted. */
    public static List<String> violations(Map<String, Set<String>> graph, String root, Set<String> modules) {
        List<String> found = new ArrayList<>();
        graph.forEach((from, uses) -> {
            String fromModule = moduleOf(from, root, modules);
            for (String to : uses) {
                String toModule = moduleOf(to, root, modules);
                if (fromModule == null || toModule == null) {
                    continue;
                }
                boolean crossing = !fromModule.equals(toModule) && !isIn(to, root + "." + toModule + ".api");
                boolean leaking = fromModule.equals(toModule)
                        && isIn(from, root + "." + fromModule + ".api")
                        && isIn(to, root + "." + fromModule + ".internal");
                if (crossing || leaking) {
                    found.add(from + " -> " + to);
                }
            }
        });
        return found.stream().distinct().sorted().toList();
    }

    private static String moduleOf(String className, String root, Set<String> modules) {
        if (!className.startsWith(root + ".")) {
            return null;
        }
        String rest = className.substring(root.length() + 1);
        int dot = rest.indexOf('.');
        String first = dot < 0 ? null : rest.substring(0, dot);
        return first != null && modules.contains(first) ? first : null;
    }

    private static boolean isIn(String className, String pkg) {
        return className.startsWith(pkg + ".");
    }
}
