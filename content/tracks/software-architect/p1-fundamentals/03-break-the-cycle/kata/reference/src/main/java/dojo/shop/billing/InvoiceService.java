package dojo.shop.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Writes and keeps invoices. */
public class InvoiceService {
    private final List<Invoice> invoices = new ArrayList<>();

    public Invoice invoice(InvoiceRequest request) {
        long net = request.netCents();
        long vat = (net * 20 + 50) / 100;
        Invoice invoice = new Invoice("INV-" + (invoices.size() + 1), request.orderId(), request.customer(), net, vat);
        invoices.add(invoice);
        return invoice;
    }

    public Optional<Invoice> find(String id) {
        return invoices.stream().filter(invoice -> invoice.id().equals(id)).findFirst();
    }

    public List<Invoice> all() {
        return List.copyOf(invoices);
    }
}
