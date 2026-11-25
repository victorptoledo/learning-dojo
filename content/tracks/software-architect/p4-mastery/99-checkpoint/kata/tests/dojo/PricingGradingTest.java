package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.legacy.pricing.PriceEngine;
import dojo.shop.pricing.api.Category;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;
import dojo.shop.pricing.legacy.LegacyPricing;
import dojo.shop.pricing.modern.ModernPricing;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PricingGradingTest {
    private static final Map<Category, String> CODES =
            Map.of(Category.BOOKS, "BK", Category.ELECTRONICS, "EL", Category.GROCERY, "GR");
    private static final long[] UNITS = {0, 1, 7, 99, 333, 999, 1000, 1001, 4999, 12345};
    private static final int[] QUANTITIES = {1, 2, 3, 4, 9, 10, 11, 25};

    private final PriceEngine engine = new PriceEngine();

    private void agreesWithTheEngine(Pricing pricing) {
        for (Category category : Category.values()) {
            for (long unit : UNITS) {
                for (int quantity : QUANTITIES) {
                    for (boolean member : new boolean[] {false, true}) {
                        long expected = engine.calc(CODES.get(category), unit, quantity, member ? "M" : "G");
                        PriceRequest request = new PriceRequest(category, unit, quantity, member);
                        assertEquals(expected, pricing.priceCents(request), request.toString());
                    }
                }
            }
        }
    }

    @Test
    void theLegacyAdapterTranslatesEveryCategoryAndCustomer() {
        agreesWithTheEngine(new LegacyPricing(engine));
    }

    @Test
    void theModernPricingGivesTheSamePriceAsTheEngineToTheCent() {
        agreesWithTheEngine(new ModernPricing());
    }

    @Test
    void someKnownPrices() {
        Pricing modern = new ModernPricing();
        assertEquals(9000, modern.priceCents(new PriceRequest(Category.BOOKS, 1000, 10, false)), "10 books: 10% off");
        assertEquals(8122, modern.priceCents(new PriceRequest(Category.BOOKS, 950, 10, true)),
                "member 5% off, then 10% off, rounding down after each step");
        assertEquals(969 + 450, modern.priceCents(new PriceRequest(Category.ELECTRONICS, 333, 3, false)),
                "3% off from three items, then the eco fee per item");
        assertEquals((999 + 300) * 90 / 100, modern.priceCents(new PriceRequest(Category.GROCERY, 999, 1, true)),
                "the handling fee comes before the member discount");
        assertEquals(1000, modern.priceCents(new PriceRequest(Category.GROCERY, 1000, 1, false)), "no fee from 1000 cents");
    }

    @Test
    void theModernPricingRefusesBadLinesLikeTheEngine() {
        Pricing modern = new ModernPricing();
        assertThrows(IllegalArgumentException.class, () -> modern.priceCents(new PriceRequest(Category.BOOKS, 100, 0, false)));
        assertThrows(IllegalArgumentException.class, () -> modern.priceCents(new PriceRequest(Category.GROCERY, -1, 1, false)));
    }
}
