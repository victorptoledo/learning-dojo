package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * These tests describe the design you are refactoring towards, so they do not compile
 * until SaleSource and the new DailyReport constructor exist (see README.md).
 */
class DailyReportTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 14);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-14T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void totals_are_printed_per_product_in_name_order() {
        SaleSource source = day -> List.of(
                new Sale("tea", day, 300),
                new Sale("coffee", day, 350),
                new Sale("coffee", day, 355));
        var report = new DailyReport(source, CLOCK);

        assertEquals("""
                Sales on 2026-03-14
                coffee: 7.05
                tea: 3.00
                TOTAL: 10.05""", report.render());
    }

    @Test
    void a_day_without_sales_says_so() {
        var report = new DailyReport(day -> List.of(), CLOCK);
        assertEquals("No sales on 2026-03-14", report.render());
    }

    @Test
    void the_totals_can_be_computed_without_any_report() {
        var totals = DailyTotals.of(DAY, List.of(new Sale("tea", DAY, 300), new Sale("tea", DAY, 250)));
        assertEquals(550, totals.grandTotal());
        assertEquals(550L, totals.byProduct().get("tea"));
    }
}
