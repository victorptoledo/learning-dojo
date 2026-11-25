package dojo.shop.checkout;

import dojo.shop.pricing.api.Category;

/** One line of a cart. */
public record CartLine(String sku, Category category, long unitPriceCents, int quantity) {}
