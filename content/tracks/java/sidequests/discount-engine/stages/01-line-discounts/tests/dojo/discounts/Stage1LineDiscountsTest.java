package dojo.discounts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class Stage1LineDiscountsTest {

    static Line line(String sku, String category, long unitCents, int quantity) {
        return new Line(sku, category, unitCents, quantity);
    }

    static Receipt price(String rules, Line... lines) {
        return DiscountEngine.parse(rules).price(List.of(lines), "standard");
    }

    @Test
    void withoutRulesNothingChanges() {
        var receipt = price("", line("B1", "books", 2500, 2), line("PEN", "office", 150, 4));
        assertEquals(new Receipt(5600, 0, 5600, List.of()), receipt);
    }

    @Test
    void aPercentOffACategory() {
        var receipt = price("percent 10 category books", line("B1", "books", 2500, 2), line("PEN", "office", 150, 4));
        assertEquals(new Receipt(5600, 500, 5100, List.of("percent 10 category books")), receipt);
    }

    @Test
    void aPercentOffASku() {
        var receipt = price("percent 25 sku PEN", line("PEN", "office", 200, 3), line("INK", "office", 400, 1));
        assertEquals(new Receipt(1000, 150, 850, List.of("percent 25 sku PEN")), receipt);
    }

    @Test
    void aFixedAmountOffEachUnitNeverBelowZero() {
        assertEquals(new Receipt(1000, 300, 700, List.of("fixed 100 sku PEN")),
                price("fixed 100 sku PEN", line("PEN", "office", 250, 3), line("INK", "office", 250, 1)));
        assertEquals(new Receipt(750, 750, 0, List.of("fixed 300 sku PEN")),
                price("fixed 300 sku PEN", line("PEN", "office", 250, 3)));
    }

    @Test
    void percentagesRoundDownToTheCent() {
        var receipt = price("percent 10 sku X", line("X", "misc", 333, 3));
        assertEquals(99, receipt.discountCents());
        assertEquals(900, receipt.totalCents());
    }

    @Test
    void aLineGetsOnlyItsLargestDiscount() {
        String rules = """
                percent 10 category books
                percent 30 sku B2
                """;
        var receipt = price(rules, line("B1", "books", 1000, 1), line("B2", "books", 1000, 1));
        assertEquals(100 + 300, receipt.discountCents(), "B2 gets 30%, not 10% + 30%");
        assertEquals(List.of("percent 10 category books", "percent 30 sku B2"), receipt.applied());
    }

    @Test
    void onlyRulesThatWonAreApplied() {
        String rules = """
                percent 5 category books
                percent 20 category books
                fixed 10 sku NOPE
                """;
        var receipt = price(rules, line("B1", "books", 1000, 1));
        assertEquals(200, receipt.discountCents());
        assertEquals(List.of("percent 20 category books"), receipt.applied());
    }

    @Test
    void onATieTheFirstRuleWins() {
        String rules = """
                fixed 100 sku A
                percent 10 sku A
                """;
        var receipt = price(rules, line("A", "misc", 1000, 1));
        assertEquals(List.of("fixed 100 sku A"), receipt.applied());
    }

    @Test
    void commentsBlankLinesAndExtraSpacesAroundARuleAreIgnored() {
        String rules = """
                # weekly promotions

                   percent 50 sku A   \s
                """;
        var receipt = price(rules, line("A", "misc", 800, 1));
        assertEquals(new Receipt(800, 400, 400, List.of("percent 50 sku A")), receipt);
    }

    @Test
    void skusAndCategoriesAreCaseSensitive() {
        var receipt = price("percent 50 category Books", line("A", "books", 800, 1));
        assertEquals(0, receipt.discountCents());
    }

    @Test
    void oneEngineCanPriceManyCarts() {
        var engine = DiscountEngine.parse("percent 10 sku A");
        assertEquals(10, engine.price(List.of(line("A", "x", 100, 1)), "standard").discountCents());
        assertEquals(20, engine.price(List.of(line("A", "x", 100, 2)), "standard").discountCents());
    }
}
