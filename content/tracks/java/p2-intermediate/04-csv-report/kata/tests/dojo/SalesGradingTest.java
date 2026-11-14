package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SalesGradingTest {
    private static final String SAMPLE = """
            # exported 2026-11-14
            2026-11-01,north,tea,3,250
            2026-11-01, south , mug ,1,1200

            2026-11-02,north,tea,2,250
            2026-11-02,east,cake,4,300
            2026-11-03,south,mug,2,1200
            """;

    private static LocalDate day(int d) {
        return LocalDate.of(2026, 11, d);
    }

    @Test
    void parses_trims_and_skips_comments_and_blank_lines() {
        var sales = SalesParser.parse(SAMPLE);
        assertEquals(5, sales.size());
        assertEquals(new Sale(day(1), "south", "mug", 1, 1200), sales.get(1));
        assertEquals(new Sale(day(3), "south", "mug", 2, 1200), sales.getLast());
    }

    @Test
    void empty_text_has_no_sales() {
        assertEquals(List.of(), SalesParser.parse(""));
        assertEquals(List.of(), SalesParser.parse("# only a comment\n\n"));
    }

    private static BadLineException bad(String text) {
        return assertThrows(BadLineException.class, () -> SalesParser.parse(text));
    }

    @Test
    void reports_the_wrong_number_of_fields_with_its_line() {
        var error = bad("# header\n2026-11-01,north,tea,3,250\n2026-11-01,north,tea,3\n");
        assertEquals(3, error.line());
        assertEquals("line 3: expected 5 fields but found 4", error.getMessage());
        assertEquals("line 1: expected 5 fields but found 6", bad("2026-11-01,north,tea,3,250,").getMessage());
    }

    @Test
    void reports_a_bad_date_and_keeps_the_cause() {
        var error = bad("2026-13-01,north,tea,3,250");
        assertEquals("line 1: bad date '2026-13-01'", error.getMessage());
        assertInstanceOf(DateTimeParseException.class, error.getCause());
    }

    @Test
    void reports_missing_text_fields() {
        assertEquals("line 1: missing region or product", bad("2026-11-01, ,tea,3,250").getMessage());
        assertEquals("line 2: missing region or product", bad("\n2026-11-01,north,,3,250").getMessage());
    }

    @Test
    void reports_bad_units_and_prices() {
        var units = bad("2026-11-01,north,tea,x,250");
        assertEquals("line 1: bad units 'x'", units.getMessage());
        assertInstanceOf(NumberFormatException.class, units.getCause());
        assertEquals("line 1: bad units '0'", bad("2026-11-01,north,tea,0,250").getMessage());
        assertEquals("line 1: bad price '-5'", bad("2026-11-01,north,tea,1,-5").getMessage());
        assertEquals("line 1: bad price '2.50'", bad("2026-11-01,north,tea,1,2.50").getMessage());
    }

    @Test
    void total_of_every_sale() {
        assertEquals(750 + 1200 + 500 + 1200 + 2400, SalesReport.totalCents(SalesParser.parse(SAMPLE)));
        assertEquals(0, SalesReport.totalCents(List.of()));
    }

    @Test
    void cents_by_region_are_sorted_by_region() {
        var byRegion = SalesReport.centsByRegion(SalesParser.parse(SAMPLE));
        assertEquals(Map.of("east", 1200L, "north", 1250L, "south", 3600L), byRegion);
        assertEquals(List.of("east", "north", "south"), new ArrayList<>(byRegion.keySet()));
    }

    @Test
    void top_products_by_units_then_name() {
        var sales = SalesParser.parse(SAMPLE);
        assertEquals(List.of("tea", "cake"), SalesReport.topProducts(sales, 2));
        assertEquals(List.of("tea", "cake", "mug"), SalesReport.topProducts(sales, 10));
        var tie = List.of(new Sale(day(1), "n", "b", 2, 1), new Sale(day(1), "n", "a", 2, 1));
        assertEquals(List.of("a", "b"), SalesReport.topProducts(tie, 2));
    }

    @Test
    void best_day_is_the_highest_total_and_the_earliest_on_a_tie() {
        assertEquals(Optional.of(day(3)), SalesReport.bestDay(SalesParser.parse(SAMPLE)));
        var tie = List.of(new Sale(day(5), "n", "x", 1, 100), new Sale(day(4), "n", "x", 1, 100));
        assertEquals(Optional.of(day(4)), SalesReport.bestDay(tie));
        assertEquals(Optional.empty(), SalesReport.bestDay(List.of()));
    }
}
