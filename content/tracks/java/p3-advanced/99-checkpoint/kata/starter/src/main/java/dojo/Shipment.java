package dojo;

/** A parcel to ship. Already written. */
public record Shipment(String orderId, String from, String to, int weightKg) {}
