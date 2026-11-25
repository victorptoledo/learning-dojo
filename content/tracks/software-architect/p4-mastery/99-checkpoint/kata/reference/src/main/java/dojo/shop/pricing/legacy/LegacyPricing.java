package dojo.shop.pricing.legacy;

import dojo.legacy.pricing.PriceEngine;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;

/** The adapter that keeps the legacy engine's strings out of the rest of the shop. */
public final class LegacyPricing implements Pricing {
    private final PriceEngine engine;

    public LegacyPricing(PriceEngine engine) {
        this.engine = engine;
    }

    @Override
    public long priceCents(PriceRequest request) {
        String category = switch (request.category()) {
            case BOOKS -> "BK";
            case ELECTRONICS -> "EL";
            case GROCERY -> "GR";
        };
        return engine.calc(category, request.unitPriceCents(), request.quantity(), request.member() ? "M" : "G");
    }
}
