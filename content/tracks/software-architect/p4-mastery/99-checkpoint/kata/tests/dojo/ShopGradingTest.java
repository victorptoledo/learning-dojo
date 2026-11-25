package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.legacy.pricing.PriceEngine;
import dojo.shop.App;
import dojo.shop.Shop;
import dojo.shop.checkout.CartLine;
import dojo.shop.checkout.Checkout;
import dojo.shop.pricing.api.Category;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.migration.Mode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopGradingTest {
    private static final List<CartLine> CART = List.of(
            new CartLine("dune", Category.BOOKS, 1299, 10),
            new CartLine("cable", Category.ELECTRONICS, 899, 3),
            new CartLine("tea", Category.GROCERY, 450, 2));

    private static long legacyTotal(boolean member) {
        PriceEngine engine = new PriceEngine();
        String type = member ? "M" : "G";
        return engine.calc("BK", 1299, 10, type) + engine.calc("EL", 899, 3, type) + engine.calc("GR", 450, 2, type);
    }

    @Test
    void checkoutAsksThePortOncePerLineWithTheCustomerType() {
        List<PriceRequest> asked = new ArrayList<>();
        Checkout checkout = new Checkout(request -> {
            asked.add(request);
            return 10;
        });
        assertEquals(30, checkout.totalCents(CART, true));
        assertEquals(List.of(
                new PriceRequest(Category.BOOKS, 1299, 10, true),
                new PriceRequest(Category.ELECTRONICS, 899, 3, true),
                new PriceRequest(Category.GROCERY, 450, 2, true)), asked);
        assertEquals(0, checkout.totalCents(List.of(), false), "an empty cart costs nothing");
    }

    @Test
    void theShopPricesExactlyAsBeforeOnLegacy() {
        Shop shop = App.create();
        for (Category category : Category.values()) {
            assertEquals(Mode.LEGACY, shop.pricing().mode(category));
        }
        assertEquals(legacyTotal(false), shop.checkout().totalCents(CART, false));
        assertEquals(legacyTotal(true), shop.checkout().totalCents(CART, true));
    }

    @Test
    void theShopMigratesEveryCategoryThroughACleanShadowRun() {
        Shop shop = App.create();
        for (Category category : Category.values()) {
            shop.pricing().route(category, Mode.SHADOW);
        }
        for (int i = 0; i < 3; i++) {
            shop.checkout().totalCents(CART, i % 2 == 0);
        }
        assertEquals(List.of(), shop.pricing().mismatches(), "the modern pricing agrees with the engine");
        for (Category category : Category.values()) {
            shop.pricing().route(category, Mode.MODERN);
        }
        assertEquals(legacyTotal(true), shop.checkout().totalCents(CART, true));
    }

    @Test
    void theReportStillWorks() {
        assertEquals(new PriceEngine().calc("GR", 450, 2, "G"), App.create().report().guestPriceCents("GR", 450, 2));
    }
}
