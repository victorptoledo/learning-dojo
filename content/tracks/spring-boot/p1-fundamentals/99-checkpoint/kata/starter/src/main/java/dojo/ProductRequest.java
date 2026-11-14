package dojo;

/** The body of POST /products. Add the constraints from the README. */
public record ProductRequest(String sku, String name, Long priceCents, String category) {}
