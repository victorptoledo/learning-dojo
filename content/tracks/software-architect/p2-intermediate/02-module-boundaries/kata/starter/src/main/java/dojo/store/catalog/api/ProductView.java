package dojo.store.catalog.api;

import dojo.store.catalog.internal.ProductRow;

/** A product as other modules see it. */
public record ProductView(String sku, String name, long priceCents, int weightGrams) {
    public static ProductView of(ProductRow row) {
        return new ProductView(row.sku(), row.title(), row.priceCents(), row.weightGrams());
    }
}
