package dojo.orders;

/** Something that happened to an order, in the past tense. */
public sealed interface DomainEvent permits OrderSubmitted, OrderCancelled {}
