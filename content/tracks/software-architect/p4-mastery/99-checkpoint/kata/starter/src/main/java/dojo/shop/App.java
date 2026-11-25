package dojo.shop;

import dojo.shop.checkout.Checkout;
import dojo.shop.reports.SalesReport;

/** The composition root. */
public final class App {
    private App() {}

    public static Shop create() {
        return new Shop(new Checkout(), new SalesReport());
    }
}
