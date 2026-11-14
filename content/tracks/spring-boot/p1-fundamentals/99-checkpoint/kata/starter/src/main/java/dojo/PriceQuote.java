package dojo;

/** The answer to a discount request. */
public record PriceQuote(String sku, long priceCents, long discountedCents, String currency) {}
