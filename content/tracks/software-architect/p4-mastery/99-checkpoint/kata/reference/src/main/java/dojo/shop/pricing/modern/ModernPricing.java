package dojo.shop.pricing.modern;

import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;

/** The new pricing rules: the same results as the legacy engine, one small rule per category. */
public final class ModernPricing implements Pricing {
    private static final long ECO_FEE_CENTS = 150;
    private static final long SMALL_ORDER_CENTS = 1000;
    private static final long HANDLING_CENTS = 300;

    @Override
    public long priceCents(PriceRequest request) {
        if (request.quantity() < 1 || request.unitPriceCents() < 0) {
            throw new IllegalArgumentException("bad line");
        }
        long base = request.unitPriceCents() * request.quantity();
        return switch (request.category()) {
            case BOOKS -> books(base, request);
            case ELECTRONICS -> electronics(base, request.quantity());
            case GROCERY -> grocery(base, request.member());
        };
    }

    private static long books(long base, PriceRequest request) {
        long price = request.member() ? percent(base, 95) : base;
        return request.quantity() >= 10 ? percent(price, 90) : price;
    }

    private static long electronics(long base, int quantity) {
        long price = quantity >= 3 ? percent(base, 97) : base;
        return price + ECO_FEE_CENTS * quantity;
    }

    private static long grocery(long base, boolean member) {
        long price = base < SMALL_ORDER_CENTS ? base + HANDLING_CENTS : base;
        return member ? percent(price, 90) : price;
    }

    /** Rounds down at every step, as the invoices always have. */
    private static long percent(long cents, int percent) {
        return cents * percent / 100;
    }
}
