package dojo;

import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DailyReportGradingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 14);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-14T23:30:00Z"), ZoneOffset.UTC);

    @Test
    void the_report_asks_the_source_for_the_day_of_the_clock() {
        List<LocalDate> asked = new ArrayList<>();
        SaleSource source = day -> {
            asked.add(day);
            return List.of(new Sale("bagel", day, 199));
        };
        String text = new DailyReport(source, CLOCK).render();
        assertEquals(List.of(DAY), asked);
        assertEquals("Sales on 2026-03-14\nbagel: 1.99\nTOTAL: 1.99", text);
    }

    @Test
    void the_clock_zone_decides_the_day() {
        Clock tokyo = CLOCK.withZone(ZoneId.of("Asia/Tokyo"));
        assertEquals("No sales on 2026-03-15", new DailyReport(day -> List.of(), tokyo).render());
    }

    @Test
    void amounts_keep_two_decimals() {
        SaleSource source = day -> List.of(new Sale("a", day, 5), new Sale("b", day, 1000), new Sale("c", day, 12345));
        assertEquals("""
                Sales on 2026-03-14
                a: 0.05
                b: 10.00
                c: 123.45
                TOTAL: 133.50""", new DailyReport(source, CLOCK).render());
    }

    @Test
    void totals_ignore_other_days_and_cannot_be_changed() {
        var totals = DailyTotals.of(DAY, List.of(
                new Sale("tea", DAY, 300), new Sale("tea", DAY.minusDays(1), 999), new Sale("jam", DAY, 450)));
        assertEquals(750, totals.grandTotal());
        assertEquals(List.of("jam", "tea"), List.copyOf(totals.byProduct().keySet()));
        assertEquals(DAY, totals.day());
        assertThrows(RuntimeException.class, () -> totals.byProduct().put("x", 1L),
                "the totals map should be unmodifiable");
    }

    @Test
    void the_archive_is_one_sale_source_among_others() {
        assertTrue(SaleSource.class.isInterface(), "SaleSource must be an interface");
        assertTrue(SaleSource.class.isAssignableFrom(SaleArchive.class), "SaleArchive must implement SaleSource");
        assertTrue(DailyTotals.class.isRecord(), "DailyTotals must be a record");
        LocalDate today = LocalDate.now();
        var sales = new SaleArchive().salesOn(today);
        assertEquals(3, sales.size());
        assertTrue(sales.stream().allMatch(sale -> sale.day().equals(today)));
    }

    @Test
    void the_report_depends_only_on_a_sale_source_and_a_clock() {
        assertEquals(Set.of(SaleSource.class, Clock.class), Set.copyOf(instanceFieldTypes(DailyReport.class)),
                "DailyReport should hold a SaleSource and a Clock, nothing else");
        String code = code("dojo.DailyReport");
        assertFalse(code.contains("LocalDate.now()"),
                "DailyReport must read the day from its Clock: LocalDate.now(clock)");
        assertEquals(1, GradingSupport.count(code, "new\\s+SaleArchive\\b"),
                "only the production wiring (withDefaults) may create a SaleArchive");
    }

    @Test
    void the_report_neither_adds_up_nor_formats() {
        String code = code("dojo.DailyReport");
        for (String forbidden : List.of("StringBuilder", "String.format", "formatted(", ".merge(", "for (")) {
            assertFalse(code.contains(forbidden),
                    "DailyReport should delegate the arithmetic to DailyTotals and the layout to TextReport; found "
                            + forbidden);
        }
        assertEquals("Sales on 2026-03-14\nx: 0.01\nTOTAL: 0.01",
                TextReport.format(DailyTotals.of(DAY, List.of(new Sale("x", DAY, 1)))));
        assertTrue(DailyReport.withDefaults().render().contains("coffee: 7.00"));
    }
}
