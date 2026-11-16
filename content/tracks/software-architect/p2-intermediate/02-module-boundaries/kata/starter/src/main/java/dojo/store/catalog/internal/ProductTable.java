package dojo.store.catalog.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The catalogue's table. Do not change it. */
public class ProductTable {
    private final Map<String, ProductRow> rows = new LinkedHashMap<>();

    public ProductTable(List<ProductRow> products) {
        products.forEach(row -> rows.put(row.sku(), row));
    }

    public Optional<ProductRow> find(String sku) {
        return Optional.ofNullable(rows.get(sku));
    }
}
