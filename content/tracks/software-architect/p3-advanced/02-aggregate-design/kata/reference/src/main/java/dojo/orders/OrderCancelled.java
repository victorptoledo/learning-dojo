package dojo.orders;

public record OrderCancelled(OrderId orderId, String reason) implements DomainEvent {}
