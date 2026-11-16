package dojo.events;

/** An integration event. {@code id} is unique per event. Do not change it. */
public record Event(String id, String type, String orderId, String customerId, long amountCents) {}
