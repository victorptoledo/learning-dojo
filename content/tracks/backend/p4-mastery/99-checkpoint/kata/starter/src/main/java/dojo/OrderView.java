package dojo;

public record OrderView(long id, String customer, OrderStatus status, String reason) {}
