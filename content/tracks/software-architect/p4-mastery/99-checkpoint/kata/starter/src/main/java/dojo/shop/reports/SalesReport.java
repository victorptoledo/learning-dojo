package dojo.shop.reports;

import dojo.legacy.pricing.PriceEngine;

/**
 * Prices the catalogue at guest prices for the monthly report. It still calls the legacy engine:
 * migrating it is next quarter's work, so it stays in the fitness baseline. Given: do not change.
 */
public final class SalesReport {
    private final PriceEngine engine = new PriceEngine();

    public long guestPriceCents(String categoryCode, long unitPriceCents, int quantity) {
        return engine.calc(categoryCode, unitPriceCents, quantity, "G");
    }
}
