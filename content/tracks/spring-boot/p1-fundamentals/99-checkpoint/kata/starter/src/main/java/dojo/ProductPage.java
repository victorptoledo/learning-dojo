package dojo;

import java.util.List;

/** One page of products, with the catalog's currency. */
public record ProductPage(List<Product> items, int page, int size, long total, String currency) {}
