package dojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Turns the names in ENRICHERS ("tax", "bulk") into enrichers, in that order. An explicit
 * registry instead of reflection: a native image sees every enricher at build time.
 */
public final class Enrichers {
    private static final Map<String, Supplier<Enricher>> REGISTRY = Map.of(
            "tax", TaxEnricher::new,
            "bulk", BulkEnricher::new);

    private Enrichers() {}

    /** The known names, sorted. */
    public static List<String> known() {
        return List.copyOf(new TreeSet<>(REGISTRY.keySet()));
    }

    public static List<Enricher> named(List<String> names) {
        List<Enricher> enrichers = new ArrayList<>();
        for (String name : names) {
            Supplier<Enricher> factory = REGISTRY.get(name);
            if (factory == null) {
                throw new IllegalArgumentException("unknown enricher '" + name + "' (known: "
                        + String.join(", ", known()) + ")");
            }
            enrichers.add(factory.get());
        }
        return List.copyOf(enrichers);
    }
}
