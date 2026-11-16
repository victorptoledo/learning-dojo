package dojo.fitness;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** The boundary rule of a modular monolith, over the graph {@link ClassGraph#scan} returns. */
public final class ModuleRules {
    private ModuleRules() {}

    /** Every use that crosses a module boundary other than through an api, sorted. */
    public static List<String> violations(Map<String, Set<String>> graph, String root, Set<String> modules) {
        throw new UnsupportedOperationException("todo: write the module boundary rule");
    }
}
