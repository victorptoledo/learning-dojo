package dojo.billing.internal;

import dojo.billing.InvoiceFormatter;
import dojo.billing.InvoiceView;

public class PlainFormatter implements InvoiceFormatter {
    @Override
    public String format(InvoiceView invoice) {
        return "Invoice %s for %s: %d.%02d EUR, due %s".formatted(invoice.number(), invoice.customer(),
                invoice.amountCents() / 100, invoice.amountCents() % 100, invoice.dueOn());
    }
}
