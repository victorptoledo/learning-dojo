package dojo.shop.checkout;

/** One line of a cart. {@code categoryCode} is the legacy engine's code: "BK", "EL" or "GR". */
public record CartLine(String sku, String categoryCode, long unitPriceCents, int quantity) {}
