package dojo.store.catalog.internal;

/** A row of the catalogue's own table. Do not change it. */
public record ProductRow(String sku, String title, long priceCents, int weightGrams, String supplierCode) {}
