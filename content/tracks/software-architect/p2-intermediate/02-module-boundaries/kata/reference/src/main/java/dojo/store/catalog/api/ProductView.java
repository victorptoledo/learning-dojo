package dojo.store.catalog.api;

/** A product as other modules see it. */
public record ProductView(String sku, String name, long priceCents, int weightGrams) {}
