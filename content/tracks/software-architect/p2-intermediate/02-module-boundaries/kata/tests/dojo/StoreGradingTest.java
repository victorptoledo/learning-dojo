package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.store.App;
import dojo.store.Store;
import dojo.store.catalog.api.ProductView;
import dojo.store.catalog.internal.ProductRow;
import dojo.store.orders.api.OrderConfirmation;
import dojo.store.shipping.api.Shipment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StoreGradingTest {
    private static final Set<String> MODULES = Set.of("catalog", "orders", "shipping");
    private static final List<ProductRow> PRODUCTS = List.of(
            new ProductRow("pen", "Pen", 150, 20, "S-1"),
            new ProductRow("lamp", "Desk lamp", 3500, 1900, "S-2"),
            new ProductRow("desk", "Desk", 25000, 19000, "S-3"));

    private final Store store = App.create(PRODUCTS);

    private static String moduleOf(String name) {
        for (String module : MODULES) {
            if (name.startsWith("dojo.store." + module + ".")) {
                return module;
            }
        }
        return null;
    }

    @Test
    void theStoreKeepsItsBoundaries() {
        Map<String, Set<String>> graph = GradingGraph.scan("dojo.store");
        assertTrue(graph.containsKey("dojo.store.orders.internal.OrderService"), "the scan found no store classes");
        List<String> bad = new ArrayList<>();
        graph.forEach((from, uses) -> uses.forEach(to -> {
            String a = moduleOf(from);
            String b = moduleOf(to);
            if (a == null || b == null) {
                return;
            }
            boolean crossing = !a.equals(b) && !to.startsWith("dojo.store." + b + ".api.");
            boolean leaking = a.equals(b) && from.startsWith("dojo.store." + a + ".api.")
                    && to.startsWith("dojo.store." + a + ".internal.");
            if (crossing || leaking) {
                bad.add(from + " -> " + to);
            }
        }));
        assertEquals(List.of(), bad);
    }

    @Test
    void theCatalogPublishesViews() {
        assertEquals(new ProductView("lamp", "Desk lamp", 3500, 1900), store.catalog().product("lamp").orElseThrow());
        assertTrue(store.catalog().product("nope").isEmpty());
    }

    @Test
    void shippingWorksThroughItsApiAlone() {
        assertEquals(new Shipment("x", "letter", 450), store.shipping().ship("x", 2000));
        assertEquals(new Shipment("y", "parcel", 900), store.shipping().ship("y", 2001));
        assertEquals(new Shipment("z", "parcel", 900), store.shipping().ship("z", 20000));
        assertEquals(new Shipment("w", "freight", 4000), store.shipping().ship("w", 20001));
        assertEquals(4, store.shipping().shipments().size());
    }

    @Test
    void ordersAddUpGoodsAndWeight() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        quantities.put("pen", 10);
        quantities.put("lamp", 1);
        assertEquals(new OrderConfirmation("o-1", 5000, "parcel", 900), store.orders().place("o-1", quantities));
        assertEquals(List.of(new Shipment("o-1", "parcel", 900)), store.shipping().shipments());
    }

    @Test
    void aHeavyOrderGoesByFreight() {
        assertEquals(new OrderConfirmation("o-2", 50000, "freight", 4000), store.orders().place("o-2", Map.of("desk", 2)));
    }

    @Test
    void badOrdersShipNothing() {
        assertThrows(IllegalArgumentException.class, () -> store.orders().place("o-3", Map.of("pen", 1, "nope", 1)));
        assertThrows(IllegalArgumentException.class, () -> store.orders().place("o-4", Map.of("pen", 0)));
        assertEquals(List.of(), store.shipping().shipments());
    }
}
