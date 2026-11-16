package dojo.shop.billing;

import java.util.HashSet;
import java.util.Set;

/** Records payments of invoices. */
public class PaymentService {
    private final InvoiceService invoices;
    private final PaymentListener listener;
    private final Set<String> paid = new HashSet<>();

    public PaymentService(InvoiceService invoices, PaymentListener listener) {
        this.invoices = invoices;
        this.listener = listener;
    }

    public void recordPayment(String invoiceId, long cents) {
        Invoice invoice = invoices.find(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("unknown invoice " + invoiceId));
        if (cents != invoice.totalCents()) {
            throw new IllegalArgumentException("amount does not match invoice " + invoiceId);
        }
        if (!paid.add(invoiceId)) {
            throw new IllegalStateException("invoice already paid: " + invoiceId);
        }
        listener.invoicePaid(invoice.orderId());
    }
}
