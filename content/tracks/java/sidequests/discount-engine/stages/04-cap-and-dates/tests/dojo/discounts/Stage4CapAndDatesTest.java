package dojo.discounts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class Stage4CapAndDatesTest {

    private static final List<Line> CART = List.of(new Line("A", "x", 10000, 1));

    @Test
    void theCapLimitsTheWholeDiscount() {
        String rules = """
                percent 30 sku A
                over 5000 off 2000
                cap 40
                """;
        var receipt = DiscountEngine.parse(rules).price(CART, "standard");
        // 3 000 + 2 000 = 5 000 would be 50%; the cap keeps 4 000
        assertEquals(new Receipt(10000, 4000, 6000,
                List.of("percent 30 sku A", "over 5000 off 2000", "cap 40")), receipt);
    }

    @Test
    void aCapThatDoesNotBiteIsNotApplied() {
        var receipt = DiscountEngine.parse("percent 10 sku A\ncap 50").price(CART, "standard");
        assertEquals(1000, receipt.discountCents());
        assertEquals(List.of("percent 10 sku A"), receipt.applied());
    }

    @Test
    void theCapRoundsDown() {
        var cart = List.of(new Line("A", "x", 999, 1));
        var receipt = DiscountEngine.parse("percent 50 sku A\ncap 10").price(cart, "standard");
        assertEquals(99, receipt.discountCents());
    }

    @Test
    void aRuleIsValidUpToItsDateInclusive() {
        var engine = DiscountEngine.parse("percent 10 sku A until 2026-11-30");
        assertEquals(1000, engine.price(CART, "standard", LocalDate.of(2026, 11, 30)).discountCents());
        var later = engine.price(CART, "standard", LocalDate.of(2026, 12, 1));
        assertEquals(new Receipt(10000, 0, 10000, List.of()), later);
    }

    @Test
    void appliedShowsTheRuleWithItsDate() {
        var engine = DiscountEngine.parse("over 5000 off 500 until 2026-12-24");
        assertEquals(List.of("over 5000 off 500 until 2026-12-24"),
                engine.price(CART, "standard", LocalDate.of(2026, 12, 1)).applied());
    }

    @Test
    void expiredRulesLeaveRoomForOthers() {
        String rules = """
                percent 50 sku A until 2026-01-31
                percent 20 sku A
                """;
        var receipt = DiscountEngine.parse(rules).price(CART, "standard", LocalDate.of(2026, 2, 1));
        assertEquals(2000, receipt.discountCents());
        assertEquals(List.of("percent 20 sku A"), receipt.applied());
    }

    @Test
    void withoutADayEveryRuleCounts() {
        var engine = DiscountEngine.parse("percent 10 sku A until 2000-01-01");
        assertEquals(1000, engine.price(CART, "standard").discountCents());
    }

    @Test
    void badDatesAndCapsAreParseErrors() {
        for (String bad : List.of("percent 10 sku A until tomorrow", "percent 10 sku A until 2026-02-30", "cap 0",
                "cap 101", "cap")) {
            var error = assertThrows(IllegalArgumentException.class, () -> DiscountEngine.parse("\n" + bad));
            assertTrue(error.getMessage().startsWith("line 2: "), bad + " -> " + error.getMessage());
        }
    }
}
