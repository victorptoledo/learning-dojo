package dojo.shop.checkout;

import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;
import java.util.List;

/** Totals a cart through the pricing port: it cannot tell which engine answers. */
public final class Checkout {
    private final Pricing pricing;

    public Checkout(Pricing pricing) {
        this.pricing = pricing;
    }

    public long totalCents(List<CartLine> lines, boolean member) {
        return lines.stream()
                .mapToLong(line -> pricing.priceCents(
                        new PriceRequest(line.category(), line.unitPriceCents(), line.quantity(), member)))
                .sum();
    }
}
