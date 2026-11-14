package dojo;

import java.util.List;
import java.util.Map;

/** Prices orders. Every amount is in cents. */
public final class Pricing {
    private Pricing() {}

    public static long subtotal(Order order) {
        throw new UnsupportedOperationException("todo subtotal");
    }

    public static long discountFor(Order order, Discount discount) {
        throw new UnsupportedOperationException("todo discount " + discount);
    }

    public static long total(Order order, List<Discount> discounts) {
        throw new UnsupportedOperationException("todo total with " + discounts);
    }

    public static Map<String, Long> subtotalByCategory(Order order) {
        throw new UnsupportedOperationException("todo subtotal by category");
    }
}
