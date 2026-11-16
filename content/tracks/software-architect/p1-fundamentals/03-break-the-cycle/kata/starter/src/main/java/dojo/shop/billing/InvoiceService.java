package dojo.shop.billing;

import dojo.shop.orders.Order;
import dojo.shop.orders.OrderLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Writes and keeps invoices. */
public class InvoiceService {
    private final List<Invoice> invoices = new ArrayList<>();

    public Invoice invoiceFor(Order order) {
        long net = 0;
        for (OrderLine line : order.lines()) {
            net += line.quantity() * line.unitPriceCents();
        }
        long vat = (net * 20 + 50) / 100;
        Invoice invoice = new Invoice("INV-" + (invoices.size() + 1), order.id(), order.customer(), net, vat);
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
