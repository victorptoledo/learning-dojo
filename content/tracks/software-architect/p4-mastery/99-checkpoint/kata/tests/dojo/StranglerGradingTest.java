package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.shop.pricing.api.Category;
import dojo.shop.pricing.api.PriceRequest;
import dojo.shop.pricing.api.Pricing;
import dojo.shop.pricing.migration.Mismatch;
import dojo.shop.pricing.migration.Mode;
import dojo.shop.pricing.migration.StranglerPricing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;
import org.junit.jupiter.api.Test;

class StranglerGradingTest {
    /** A fake engine that remembers what it was asked. */
    private static final class Counting implements Pricing {
        private final ToLongFunction<PriceRequest> answer;
        private final List<PriceRequest> asked = new ArrayList<>();

        Counting(ToLongFunction<PriceRequest> answer) {
            this.answer = answer;
        }

        @Override
        public long priceCents(PriceRequest request) {
            asked.add(request);
            return answer.applyAsLong(request);
        }
    }

    private final Counting legacy = new Counting(request -> request.unitPriceCents() * request.quantity());
    private final Counting modern = new Counting(request -> request.unitPriceCents() * request.quantity());
    private final StranglerPricing pricing = new StranglerPricing(legacy, modern);

    private static PriceRequest books(long unit) {
        return new PriceRequest(Category.BOOKS, unit, 1, false);
    }

    private void shadowRuns(Category category, int times) {
        for (int i = 0; i < times; i++) {
            pricing.priceCents(new PriceRequest(category, 100 + i, 1, false));
        }
    }

    @Test
    void everyCategoryStartsOnLegacyAndModernIsNeverAsked() {
        for (Category category : Category.values()) {
            assertEquals(Mode.LEGACY, pricing.mode(category));
        }
        assertEquals(500, pricing.priceCents(books(500)));
        assertEquals(1, legacy.asked.size());
        assertEquals(List.of(), modern.asked);
    }

    @Test
    void aShadowRunAsksBothAndAnswersWithLegacy() {
        Counting wrong = new Counting(request -> 1);
        StranglerPricing shadowed = new StranglerPricing(legacy, wrong);
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        assertEquals(700, shadowed.priceCents(books(700)));
        assertEquals(1, wrong.asked.size());
        assertEquals(List.of(new Mismatch(books(700), 700, "1")), shadowed.mismatches());
    }

    @Test
    void aMatchingShadowRunLogsNothing() {
        pricing.route(Category.BOOKS, Mode.SHADOW);
        assertEquals(700, pricing.priceCents(books(700)));
        assertEquals(List.of(), pricing.mismatches());
        assertEquals(1, modern.asked.size());
    }

    @Test
    void aFailingModernNeverFailsTheCustomer() {
        StranglerPricing shadowed = new StranglerPricing(legacy, request -> {
            throw new IllegalStateException("rule missing");
        });
        shadowed.route(Category.GROCERY, Mode.SHADOW);
        PriceRequest request = new PriceRequest(Category.GROCERY, 250, 2, true);
        assertEquals(500, shadowed.priceCents(request));
        assertEquals(List.of(new Mismatch(request, 500, "error: rule missing")), shadowed.mismatches());
    }

    @Test
    void aFailingLegacyStillFailsAndModernIsNotAsked() {
        StranglerPricing shadowed = new StranglerPricing(request -> {
            throw new IllegalArgumentException("bad line");
        }, modern);
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        assertThrows(IllegalArgumentException.class, () -> shadowed.priceCents(books(1)));
        assertEquals(List.of(), modern.asked);
        assertEquals(List.of(), shadowed.mismatches());
    }

    @Test
    void routesArePerCategory() {
        pricing.route(Category.ELECTRONICS, Mode.SHADOW);
        pricing.priceCents(books(10));
        assertEquals(List.of(), modern.asked, "books are still legacy only");
        assertEquals(Mode.LEGACY, pricing.mode(Category.BOOKS));
        assertEquals(Mode.SHADOW, pricing.mode(Category.ELECTRONICS));
    }

    @Test
    void cuttingOverNeedsThreeCleanShadowRuns() {
        IllegalStateException fromLegacy = assertThrows(IllegalStateException.class,
                () -> pricing.route(Category.BOOKS, Mode.MODERN));
        assertEquals("BOOKS needs a clean shadow run first", fromLegacy.getMessage());
        pricing.route(Category.BOOKS, Mode.SHADOW);
        shadowRuns(Category.BOOKS, 2);
        assertThrows(IllegalStateException.class, () -> pricing.route(Category.BOOKS, Mode.MODERN), "two runs are not enough");
        assertEquals(Mode.SHADOW, pricing.mode(Category.BOOKS), "a refused cut-over changes nothing");
        shadowRuns(Category.BOOKS, 1);
        pricing.route(Category.BOOKS, Mode.MODERN);
        assertEquals(Mode.MODERN, pricing.mode(Category.BOOKS));
    }

    @Test
    void otherCategoriesRunsDoNotCount() {
        pricing.route(Category.BOOKS, Mode.SHADOW);
        pricing.route(Category.GROCERY, Mode.SHADOW);
        shadowRuns(Category.GROCERY, 5);
        assertThrows(IllegalStateException.class, () -> pricing.route(Category.BOOKS, Mode.MODERN));
    }

    @Test
    void aMismatchBlocksTheCutOverUntilANewShadowRun() {
        Counting flaky = new Counting(request -> request.unitPriceCents() == 101 ? 0 : request.unitPriceCents());
        StranglerPricing shadowed = new StranglerPricing(legacy, flaky);
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        for (long unit = 100; unit < 106; unit++) {
            shadowed.priceCents(books(unit));
        }
        assertThrows(IllegalStateException.class, () -> shadowed.route(Category.BOOKS, Mode.MODERN));
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        for (long unit = 102; unit < 105; unit++) {
            shadowed.priceCents(books(unit));
        }
        assertThrows(IllegalStateException.class, () -> shadowed.route(Category.BOOKS, Mode.MODERN),
                "staying in shadow does not forget the mismatch");
        shadowed.route(Category.BOOKS, Mode.LEGACY);
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        for (long unit = 102; unit < 105; unit++) {
            shadowed.priceCents(books(unit));
        }
        shadowed.route(Category.BOOKS, Mode.MODERN);
        assertEquals(1, shadowed.mismatches().size(), "the log keeps the history");
    }

    @Test
    void afterTheCutOverOnlyModernAnswersAndRollbackIsAlwaysAllowed() {
        pricing.route(Category.BOOKS, Mode.SHADOW);
        shadowRuns(Category.BOOKS, 3);
        pricing.route(Category.BOOKS, Mode.MODERN);
        int legacyCalls = legacy.asked.size();
        assertEquals(42, pricing.priceCents(books(42)));
        assertEquals(legacyCalls, legacy.asked.size(), "legacy is not asked any more");
        pricing.route(Category.BOOKS, Mode.LEGACY);
        assertEquals(Mode.LEGACY, pricing.mode(Category.BOOKS));
        assertThrows(IllegalStateException.class, () -> pricing.route(Category.BOOKS, Mode.MODERN),
                "after a rollback, cutting over needs a new shadow run");
    }

    @Test
    void theMismatchLogCannotBeChangedFromOutside() {
        StranglerPricing shadowed = new StranglerPricing(legacy, request -> 1);
        shadowed.route(Category.BOOKS, Mode.SHADOW);
        shadowed.priceCents(books(5));
        try {
            shadowed.mismatches().clear();
        } catch (UnsupportedOperationException fine) {
            // an unmodifiable copy is fine too
        }
        assertEquals(1, shadowed.mismatches().size());
    }
}
