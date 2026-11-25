package dojo.shop.pricing.api;

/** The port: the price of one cart line, in cents. */
public interface Pricing {
    long priceCents(PriceRequest request);
}
