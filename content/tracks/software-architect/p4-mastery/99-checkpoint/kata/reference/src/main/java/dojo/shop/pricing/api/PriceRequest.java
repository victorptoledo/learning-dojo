package dojo.shop.pricing.api;

public record PriceRequest(Category category, long unitPriceCents, int quantity, boolean member) {}
