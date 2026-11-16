package dojo;

/** A product of the catalog. Prices are in cents to avoid floating point. */
public record Product(long id, String name, String category, long priceCents) {}
