package dojo;

import java.util.StringJoiner;

/** How the totals look on paper. Changing the layout only touches this class. */
public final class TextReport {
    private TextReport() {}

    public static String format(DailyTotals totals) {
        if (totals.byProduct().isEmpty()) {
            return "No sales on " + totals.day();
        }
        var out = new StringJoiner("\n");
        out.add("Sales on " + totals.day());
        totals.byProduct().forEach((product, cents) -> out.add(product + ": " + money(cents)));
        out.add("TOTAL: " + money(totals.grandTotal()));
        return out.toString();
    }

    static String money(long cents) {
        return "%d.%02d".formatted(cents / 100, cents % 100);
    }
}
