package dojo.discounts;

import java.util.List;

/** Prices carts with discount rules read from text. See README.md. */
public final class DiscountEngine {

    private DiscountEngine() {
    }

    /** Reads the rules, one per line. */
    public static DiscountEngine parse(String rules) {
        return new DiscountEngine();
    }

    /** Prices the cart for a customer of `tier`. */
    public Receipt price(List<Line> lines, String tier) {
        throw new UnsupportedOperationException("todo: price the cart");
    }
}
