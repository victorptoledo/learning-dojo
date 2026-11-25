package dojo.billing;

import java.time.LocalDate;

/** Published by billing when an invoice is issued: everything another module needs to know. */
public record InvoiceIssued(String number, String customer, long amountCents, LocalDate dueOn) {
}
