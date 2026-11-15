package dojo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Prints today's sales per product. It works, but it decides where the sales come from,
 * what day it is, how to add them up and how to print them, all in one method, so nothing
 * here can be tested without the real database and the real clock.
 */
public class DailyReport {
    public String render() {
        LocalDate today = LocalDate.now();
        List<Sale> all = new SaleArchive().all();
        Map<String, Long> totals = new TreeMap<>();
        long grand = 0;
        for (Sale sale : all) {
            if (sale.day().equals(today)) {
                totals.merge(sale.product(), sale.cents(), Long::sum);
                grand += sale.cents();
            }
        }
        if (totals.isEmpty()) {
            return "No sales on " + today;
        }
        StringBuilder out = new StringBuilder("Sales on " + today + "\n");
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            long cents = entry.getValue();
            out.append(entry.getKey()).append(": ")
                    .append(cents / 100).append('.').append(String.format("%02d", cents % 100))
                    .append('\n');
        }
        out.append("TOTAL: ").append(grand / 100).append('.').append(String.format("%02d", grand % 100));
        return out.toString();
    }

    public static void main(String[] args) {
        System.out.println(new DailyReport().render());
    }
}
