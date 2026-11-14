package dojo;

import java.time.LocalDate;

/** One sale: {@code units} of {@code product} at {@code unitCents} each. */
public record Sale(LocalDate date, String region, String product, int units, long unitCents) {
    public long totalCents() {
        return units * unitCents;
    }
}
