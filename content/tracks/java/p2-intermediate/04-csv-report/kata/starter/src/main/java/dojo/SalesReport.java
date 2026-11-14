package dojo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Summaries of a list of sales. */
public final class SalesReport {
    private SalesReport() {}

    public static long totalCents(List<Sale> sales) {
        throw new UnsupportedOperationException("todo total of " + sales.size() + " sales");
    }

    public static Map<String, Long> centsByRegion(List<Sale> sales) {
        throw new UnsupportedOperationException("todo cents by region");
    }

    public static List<String> topProducts(List<Sale> sales, int n) {
        throw new UnsupportedOperationException("todo top " + n + " products");
    }

    public static Optional<LocalDate> bestDay(List<Sale> sales) {
        throw new UnsupportedOperationException("todo best day");
    }
}
