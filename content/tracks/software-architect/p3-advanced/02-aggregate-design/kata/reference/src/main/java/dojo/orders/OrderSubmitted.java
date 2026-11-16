package dojo.orders;

public record OrderSubmitted(OrderId orderId, CustomerId customerId, Money total) implements DomainEvent {}
