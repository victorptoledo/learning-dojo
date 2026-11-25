package dojo.billing;

import dojo.billing.internal.Invoice;
import java.time.LocalDate;

public record InvoiceView(String number, String customer, long amountCents, LocalDate issuedOn, LocalDate dueOn) {
    public static InvoiceView of(Invoice invoice) {
        return new InvoiceView(invoice.getNumber(), invoice.getCustomer(), invoice.getAmountCents(),
                invoice.getIssuedOn(), invoice.getDueOn());
    }
}
