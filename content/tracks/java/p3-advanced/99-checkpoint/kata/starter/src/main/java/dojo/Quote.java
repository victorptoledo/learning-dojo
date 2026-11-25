package dojo;

/** One carrier's offer for a shipment. Already written. */
public record Quote(String carrier, long priceCents, int days) {}
