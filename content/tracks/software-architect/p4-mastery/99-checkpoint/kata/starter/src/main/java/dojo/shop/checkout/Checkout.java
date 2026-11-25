package dojo.shop.checkout;

import dojo.legacy.pricing.PriceEngine;
import java.util.List;

/** Totals a cart. It calls the legacy engine straight away. */
public final class Checkout {
    private final PriceEngine engine = new PriceEngine();

    public long totalCents(List<CartLine> lines, boolean member) {
        long total = 0;
        for (CartLine line : lines) {
            total += engine.calc(line.categoryCode(), line.unitPriceCents(), line.quantity(), member ? "M" : "G");
        }
        return total;
    }
}
