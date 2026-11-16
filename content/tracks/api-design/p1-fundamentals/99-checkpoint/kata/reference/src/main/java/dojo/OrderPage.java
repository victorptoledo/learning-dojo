package dojo;

import java.util.List;

/** One page of a filtered, sorted orders collection. */
public record OrderPage(List<Order> items, int page, int size, long totalItems, int totalPages) {}
