package dojo.discounts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class Stage2QuantityDealsTest {

    static Receipt price(String rules, Line... lines) {
        return DiscountEngine.parse(rules).price(List.of(lines), "standard");
    }

    @Test
    void buyXGetYMakesWholeGroupsFree() {
        var receipt = price("buy 2 get 1 sku PEN", new Line("PEN", "office", 100, 7));
        assertEquals(new Receipt(700, 200, 500, List.of("buy 2 get 1 sku PEN")), receipt);
        assertEquals(0, price("buy 2 get 1 sku PEN", new Line("PEN", "office", 100, 2)).discountCents());
    }

    @Test
    void bulkLowersTheUnitPriceFromTheMinimum() {
        String rule = "bulk 10 price 450 sku PAPER";
        assertEquals(600, price(rule, new Line("PAPER", "office", 500, 12)).discountCents());
        assertEquals(500, price(rule, new Line("PAPER", "office", 500, 10)).discountCents());
        assertEquals(0, price(rule, new Line("PAPER", "office", 500, 9)).discountCents());
        assertEquals(0, price("bulk 2 price 900 sku P", new Line("P", "x", 500, 5)).discountCents(),
                "a bulk price above the unit price changes nothing");
    }

    @Test
    void dealsCompeteWithPercentagesForTheLargestDiscount() {
        String rules = """
                percent 10 category office
                buy 3 get 1 sku PEN
                """;
        var many = price(rules, new Line("PEN", "office", 100, 8));
        assertEquals(200, many.discountCents(), "2 free pens beat 10% of 800");
        assertEquals(List.of("buy 3 get 1 sku PEN"), many.applied());
        var few = price(rules, new Line("PEN", "office", 100, 3));
        assertEquals(30, few.discountCents(), "no full group: 10% wins");
    }

    private static String parseError(String rules) {
        var error = assertThrows(IllegalArgumentException.class, () -> DiscountEngine.parse(rules));
        return error.getMessage();
    }

    @Test
    void anUnknownRuleNamesItsLine() {
        String message = parseError("percent 10 sku A\n\nteleport 3 sku A\n");
        assertTrue(message.startsWith("line 3: "), message);
    }

    @Test
    void wrongWordsOrCountsAreErrors() {
        for (String bad : List.of(
                "buy 2 get sku A",
                "percent 10 sku",
                "percent 10 shelf A",
                "bulk 10 cost 450 sku A",
                "fixed 100 sku A B")) {
            String message = parseError("# rules\n" + bad);
            assertTrue(message.startsWith("line 2: "), bad + " -> " + message);
        }
    }

    @Test
    void numbersMustBePositiveWholeNumbers() {
        for (String bad : List.of("percent ten sku A", "percent 0 sku A", "fixed -5 sku A", "buy 2.5 get 1 sku A")) {
            String message = parseError(bad);
            assertTrue(message.startsWith("line 1: "), bad + " -> " + message);
        }
    }

    @Test
    void aPercentAbove100IsAnError() {
        assertTrue(parseError("percent 150 sku A").startsWith("line 1: "));
        assertEquals(100, price("percent 100 sku A", new Line("A", "x", 100, 1)).discountCents());
    }
}
