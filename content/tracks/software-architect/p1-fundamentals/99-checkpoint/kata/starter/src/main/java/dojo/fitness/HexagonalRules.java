package dojo.fitness;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** The dependency rule of a hexagon, as code over the graph {@link ClassGraph#scan} returns. */
public final class HexagonalRules {
    private HexagonalRules() {}

    /** Every forbidden use under {@code root}, as sorted {@code "from -> to"} lines. */
    public static List<String> violations(Map<String, Set<String>> graph, String root) {
        throw new UnsupportedOperationException("todo: write the three rules");
    }
}
