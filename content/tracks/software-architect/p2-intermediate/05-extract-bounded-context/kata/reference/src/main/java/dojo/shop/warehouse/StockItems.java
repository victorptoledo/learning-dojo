package dojo.shop.warehouse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** The warehouse's own store. */
public class StockItems {
    private final Map<String, StockItem> items = new LinkedHashMap<>();

    public void save(StockItem item) {
        items.put(item.sku(), item);
    }

    public Optional<StockItem> find(String sku) {
        return Optional.ofNullable(items.get(sku));
    }
}
