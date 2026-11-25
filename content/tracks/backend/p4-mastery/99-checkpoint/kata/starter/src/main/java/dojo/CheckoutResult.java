package dojo;

/** {@code path} is "saga" or "legacy": which implementation served the checkout. */
public record CheckoutResult(long orderId, OrderStatus status, String reason, String path) {}
