package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.legacy.pricing.PriceEngine;
import dojo.shop.pricing.api.Category;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.legacy.LegacyPricing;
import dojo.shop.pricing.migration.Mode;
import dojo.shop.pricing.migration.StranglerPricing;
import dojo.shop.pricing.modern.ModernPricing;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final LegacyPricing legacy = new LegacyPricing(new PriceEngine());
    private final ModernPricing modern = new ModernPricing();

    @Test
    void modernAgreesWithLegacyOnAFewLines() {
        for (Category category : Category.values()) {
            for (int quantity : new int[] {1, 3, 10}) {
                PriceRequest request = new PriceRequest(category, 333, quantity, true);
                assertEquals(legacy.priceCents(request), modern.priceCents(request), request.toString());
            }
        }
    }

    @Test
    void aShadowRunAnswersWithLegacyAndLogsDifferences() {
        StranglerPricing pricing = new StranglerPricing(legacy, request -> 1);
        pricing.route(Category.BOOKS, Mode.SHADOW);
        PriceRequest request = new PriceRequest(Category.BOOKS, 1000, 1, false);
        assertEquals(1000, pricing.priceCents(request));
        assertEquals(1, pricing.mismatches().size());
        assertEquals(List.of(1000L), pricing.mismatches().stream().map(m -> m.legacyCents()).toList());
    }
}
