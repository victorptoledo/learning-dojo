package dojo.shop;

import dojo.legacy.pricing.PriceEngine;
import dojo.shop.checkout.Checkout;
import dojo.shop.pricing.legacy.LegacyPricing;
import dojo.shop.pricing.migration.StranglerPricing;
import dojo.shop.pricing.modern.ModernPricing;
import dojo.shop.reports.SalesReport;

/** The composition root: the one place that knows every implementation. */
public final class App {
    private App() {}

    public static Shop create() {
        StranglerPricing pricing = new StranglerPricing(new LegacyPricing(new PriceEngine()), new ModernPricing());
        return new Shop(new Checkout(pricing), pricing, new SalesReport());
    }
}
