package dojo;

/** One parsed order line. Already written. */
public record Order(String id, String customerId, String sku, int quantity, long priceCents) {}
