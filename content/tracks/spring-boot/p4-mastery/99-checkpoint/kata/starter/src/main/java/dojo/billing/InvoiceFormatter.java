package dojo.billing;

/** Turns an invoice into printable text. The implementations live in billing.internal. */
public interface InvoiceFormatter {
    String format(InvoiceView invoice);
}
