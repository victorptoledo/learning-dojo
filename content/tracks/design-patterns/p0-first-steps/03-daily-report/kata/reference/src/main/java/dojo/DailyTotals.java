package dojo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/** The arithmetic of the report, with no I/O: easy to test on its own. */
public record DailyTotals(LocalDate day, SortedMap<String, Long> byProduct) {
    public DailyTotals {
        byProduct = Collections.unmodifiableSortedMap(new TreeMap<>(byProduct));
    }

    /** Totals per product of the sales made on {@code day}; sales of other days are ignored. */
    public static DailyTotals of(LocalDate day, List<Sale> sales) {
        SortedMap<String, Long> totals = new TreeMap<>();
        for (Sale sale : sales) {
            if (sale.day().equals(day)) {
                totals.merge(sale.product(), sale.cents(), Long::sum);
            }
        }
        return new DailyTotals(day, totals);
    }

    public long grandTotal() {
        return byProduct.values().stream().mapToLong(Long::longValue).sum();
    }
}
