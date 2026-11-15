package dojo;

import java.util.List;

/** The built-in pricing rules. */
public final class PricingSteps {
    private PricingSteps() {}

    public static PricingStep bulkDiscount() {
        return (request, quote) -> request.quantity() >= 10
                ? quote.adjusted(quote.cents() - quote.cents() * 5 / 100, "bulk -5%")
                : quote;
    }

    public static PricingStep vipDiscount() {
        return (request, quote) -> request.vip()
                ? quote.adjusted(quote.cents() - quote.cents() * 10 / 100, "vip -10%")
                : quote;
    }

    public static PricingStep coupon() {
        return (request, quote) -> "SAVE5".equals(request.coupon())
                ? quote.adjusted(Math.max(0, quote.cents() - 500), "coupon SAVE5")
                : quote;
    }

    public static PricingStep expressShipping() {
        return (request, quote) -> request.express() ? quote.adjusted(quote.cents() + 1500, "express +15.00") : quote;
    }

    public static PricingStep tax() {
        return (request, quote) -> quote.adjusted(quote.cents() + (quote.cents() * 23 + 50) / 100, "tax 23%");
    }

    /** Today's rules, in today's order. */
    public static List<PricingStep> standard() {
        return List.of(bulkDiscount(), vipDiscount(), coupon(), expressShipping(), tax());
    }
}
