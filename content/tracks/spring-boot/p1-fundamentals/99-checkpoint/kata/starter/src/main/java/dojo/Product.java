package dojo;

/** A product of the catalog. Prices are in cents. */
public record Product(String sku, String name, long priceCents, String category) {}
