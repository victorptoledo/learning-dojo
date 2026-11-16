package dojo.store.shipping.api;

public record Shipment(String orderId, String carrier, long costCents) {}
