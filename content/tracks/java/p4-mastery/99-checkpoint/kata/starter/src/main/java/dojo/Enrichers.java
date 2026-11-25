package dojo;

import java.util.ArrayList;
import java.util.List;

/** Turns the names in ENRICHERS ("tax", "bulk") into enrichers, in that order. */
public final class Enrichers {
    private Enrichers() {}

    public static List<Enricher> named(List<String> names) {
        List<Enricher> enrichers = new ArrayList<>();
        for (String name : names) {
            String className = "dojo." + Character.toUpperCase(name.charAt(0)) + name.substring(1) + "Enricher";
            try {
                enrichers.add((Enricher) Class.forName(className).getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("cannot load enricher " + name, e);
            }
        }
        return enrichers;
    }
}
