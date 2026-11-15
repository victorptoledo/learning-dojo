package dojo;

import java.time.LocalDate;
import java.util.List;

/**
 * Stands in for the shop's sales database. In real life this would run a query; here it
 * returns a fixed list. Leave the data as it is.
 */
public class SaleArchive implements SaleSource {
    public List<Sale> all() {
        LocalDate today = LocalDate.now();
        return List.of(
                new Sale("coffee", today, 350),
                new Sale("croissant", today, 280),
                new Sale("coffee", today, 350),
                new Sale("tea", today.minusDays(1), 300));
    }

    @Override
    public List<Sale> salesOn(LocalDate day) {
        return all().stream().filter(sale -> sale.day().equals(day)).toList();
    }
}
