package dojo.shop.billing;

import dojo.shop.orders.OrderRepository;
import java.util.HashSet;
import java.util.Set;

/** Records payments of invoices. */
public class PaymentService {
    private final InvoiceService invoices;
    private final OrderRepository orders;
    private final Set<String> paid = new HashSet<>();

    public PaymentService(InvoiceService invoices, OrderRepository orders) {
        this.invoices = invoices;
        this.orders = orders;
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
        orders.find(invoice.orderId()).orElseThrow().markPaid();
    }
}
