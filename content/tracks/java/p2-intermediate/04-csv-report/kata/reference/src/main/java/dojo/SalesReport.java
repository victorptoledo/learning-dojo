package dojo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Summaries of a list of sales. */
public final class SalesReport {
    private SalesReport() {}

    public static long totalCents(List<Sale> sales) {
        return sales.stream().mapToLong(Sale::totalCents).sum();
    }

    public static Map<String, Long> centsByRegion(List<Sale> sales) {
        return sales.stream()
                .collect(Collectors.groupingBy(Sale::region, TreeMap::new, Collectors.summingLong(Sale::totalCents)));
    }

    public static List<String> topProducts(List<Sale> sales, int n) {
        Map<String, Integer> units = sales.stream()
                .collect(Collectors.groupingBy(Sale::product, Collectors.summingInt(Sale::units)));
        return units.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static Optional<LocalDate> bestDay(List<Sale> sales) {
        Map<LocalDate, Long> byDay = sales.stream()
                .collect(Collectors.groupingBy(Sale::date, TreeMap::new, Collectors.summingLong(Sale::totalCents)));
        return byDay.entrySet().stream()
                .max(Map.Entry.<LocalDate, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey);
    }
}
