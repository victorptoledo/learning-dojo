package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.shop.sales.Product;
import dojo.shop.warehouse.StockItem;
import dojo.shop.warehouse.WarehouseService;
import dojo.shop.warehouse.api.Stock;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ContextsGradingTest {
    private static final Map<String, Set<String>> SHOP = GradingGraph.scan("dojo.shop");

    private static List<String> uses(String from, java.util.function.Predicate<String> forbidden) {
        return SHOP.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(from))
                .flatMap(entry -> entry.getValue().stream().filter(forbidden)
                        .map(used -> entry.getKey() + " -> " + used))
                .toList();
    }

    private static List<String> components(Class<?> type) {
        assertTrue(type.isRecord(), type.getSimpleName() + " should be a record");
        return Arrays.stream(type.getRecordComponents())
                .map(c -> c.getType().getSimpleName() + " " + c.getName())
                .toList();
    }

    @Test
    void salesUsesOnlyTheWarehouseApi() {
        assertTrue(SHOP.containsKey("dojo.shop.sales.SalesService"), "the scan found no shop classes");
        assertEquals(List.of(), uses("dojo.shop.sales.", to -> to.startsWith("dojo.shop.catalog.")
                || to.startsWith("dojo.shop.warehouse.") && !to.startsWith("dojo.shop.warehouse.api.")));
    }

    @Test
    void theWarehouseUsesNeitherSalesNorTheLegacyCatalog() {
        assertEquals(List.of(), uses("dojo.shop.warehouse.", to -> to.startsWith("dojo.shop.catalog.")
                || to.startsWith("dojo.shop.sales.")));
    }

    @Test
    void eachContextHasItsOwnModel() {
        assertEquals(List.of("String sku", "String name", "long priceCents", "boolean discontinued"),
                components(Product.class));
        assertEquals(List.of("String sku", "int weightGrams", "String bin", "int onHand"),
                components(StockItem.class));
    }

    @Test
    void theWarehousePublishesStock() {
        assertTrue(Stock.class.isInterface(), "Stock should be an interface");
        assertTrue(Stock.class.isAssignableFrom(WarehouseService.class), "WarehouseService should implement Stock");
    }
}
