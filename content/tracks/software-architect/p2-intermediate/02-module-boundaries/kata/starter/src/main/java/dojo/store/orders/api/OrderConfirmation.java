package dojo.store.orders.api;

public record OrderConfirmation(String orderId, long goodsCents, String carrier, long shippingCents) {}
