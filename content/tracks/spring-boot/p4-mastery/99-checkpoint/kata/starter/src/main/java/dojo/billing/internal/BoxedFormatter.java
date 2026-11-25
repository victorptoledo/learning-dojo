package dojo.billing.internal;

import dojo.billing.InvoiceFormatter;
import dojo.billing.InvoiceView;

public class BoxedFormatter implements InvoiceFormatter {
    @Override
    public String format(InvoiceView invoice) {
        String line = "| Invoice %s | %s | %d.%02d EUR | due %s |".formatted(invoice.number(), invoice.customer(),
                invoice.amountCents() / 100, invoice.amountCents() % 100, invoice.dueOn());
        String rule = "+" + "-".repeat(line.length() - 2) + "+";
        return rule + "\n" + line + "\n" + rule;
    }
}
