package dojo.store.catalog.internal;

import dojo.store.catalog.api.Catalog;
import dojo.store.catalog.api.ProductView;
import java.util.Optional;

public class CatalogService implements Catalog {
    private final ProductTable table;

    public CatalogService(ProductTable table) {
        this.table = table;
    }

    @Override
    public Optional<ProductView> product(String sku) {
        return table.find(sku).map(row -> new ProductView(row.sku(), row.title(), row.priceCents(), row.weightGrams()));
    }
}
