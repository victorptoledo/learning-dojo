package dojo.shop.pricing.migration;

import dojo.shop.pricing.api.PriceRequest;

/** A shadow run where the modern price differed: {@code modern} is the price or {@code "error: ..."}. */
public record Mismatch(PriceRequest request, long legacyCents, String modern) {}
