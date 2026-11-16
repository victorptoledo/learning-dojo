package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.fitness.ClassGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
    private static final Map<String, Set<String>> SHOP = ClassGraph.scan("dojo.shop");

    private static List<String> uses(String from, String to) {
        return SHOP.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(from))
                .flatMap(entry -> entry.getValue().stream().filter(used -> used.startsWith(to))
                        .map(used -> entry.getKey() + " -> " + used))
                .toList();
    }

    @Test
    void noContextUsesTheLegacyCatalog() {
        assertEquals(List.of(), uses("dojo.shop.sales.", "dojo.shop.catalog."));
        assertEquals(List.of(), uses("dojo.shop.warehouse.", "dojo.shop.catalog."));
    }

    @Test
    void theWarehouseKnowsNothingOfSales() {
        assertEquals(List.of(), uses("dojo.shop.warehouse.", "dojo.shop.sales."));
    }
}
