package dojo.discounts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class Stage3CartRulesTest {

    static Receipt price(String rules, String tier, Line... lines) {
        return DiscountEngine.parse(rules).price(List.of(lines), tier);
    }

    @Test
    void overTakesCentsOffACartAboveTheThreshold() {
        String rule = "over 10000 off 1500";
        assertEquals(new Receipt(12000, 1500, 10500, List.of(rule)),
                price(rule, "standard", new Line("A", "x", 6000, 2)));
        assertEquals(0, price(rule, "standard", new Line("A", "x", 9999, 1)).discountCents());
        assertEquals(1500, price(rule, "standard", new Line("A", "x", 10000, 1)).discountCents(),
                "the threshold itself counts");
    }

    @Test
    void overLooksAtTheCartAfterLineDiscounts() {
        String rules = """
                percent 10 sku A
                over 10000 off 1500
                """;
        var receipt = price(rules, "standard", new Line("A", "x", 10500, 1));
        assertEquals(1050, receipt.discountCents(), "10 500 - 1 050 = 9 450 is under the threshold");
        assertEquals(List.of("percent 10 sku A"), receipt.applied());
    }

    @Test
    void onlyTheBestOverRuleCounts() {
        String rules = """
                over 5000 off 500
                over 10000 off 2000
                over 20000 off 5000
                """;
        var receipt = price(rules, "standard", new Line("A", "x", 15000, 1));
        assertEquals(2000, receipt.discountCents());
        assertEquals(List.of("over 10000 off 2000"), receipt.applied());
    }

    @Test
    void theTierPercentComesLastAndIgnoresCase() {
        String rules = """
                tier gold percent 10
                percent 10 sku A
                over 10000 off 1500
                tier silver percent 5
                """;
        var receipt = price(rules, "Gold", new Line("A", "x", 6000, 2));
        // 12 000 - 1 200 = 10 800, - 1 500 = 9 300, - 930 = 8 370
        assertEquals(new Receipt(12000, 3630, 8370,
                List.of("tier gold percent 10", "percent 10 sku A", "over 10000 off 1500")), receipt);
    }

    @Test
    void anotherTierGetsNothing() {
        var receipt = price("tier gold percent 10", "bronze", new Line("A", "x", 1000, 1));
        assertEquals(0, receipt.discountCents());
        assertEquals(List.of(), receipt.applied());
    }

    @Test
    void theBestTierRuleOfTheCustomerCounts() {
        String rules = """
                tier gold percent 5
                tier gold percent 15
                """;
        var receipt = price(rules, "gold", new Line("A", "x", 1000, 1));
        assertEquals(150, receipt.discountCents());
        assertEquals(List.of("tier gold percent 15"), receipt.applied());
    }

    @Test
    void theTotalNeverGoesBelowZero() {
        String rules = """
                fixed 900 sku A
                over 100 off 5000
                """;
        var receipt = price(rules, "standard", new Line("A", "x", 1000, 1));
        assertEquals(new Receipt(1000, 1000, 0, List.of("fixed 900 sku A", "over 100 off 5000")), receipt);
    }

    @Test
    void badCartRulesAreParseErrors() {
        for (String bad : List.of("over 100 off", "tier gold 10", "tier gold percent 120")) {
            var error = org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class, () -> DiscountEngine.parse(bad));
            org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().startsWith("line 1: "), bad);
        }
    }
}
