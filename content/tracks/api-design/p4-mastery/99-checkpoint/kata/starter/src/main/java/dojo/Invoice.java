package dojo;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An invoice, as the domain keeps it: money in minor units (cents) with its currency, a due
 * date (a calendar day, no time) and the instant it was issued. Written for you.
 */
public record Invoice(long id, String tenant, String customer, long amountCents, String currency,
                      LocalDate dueDate, Instant issuedAt, String status) {
    public static final String OPEN = "OPEN";
    public static final String PAID = "PAID";

    public Invoice paid() {
        return new Invoice(id, tenant, customer, amountCents, currency, dueDate, issuedAt, PAID);
    }
}
