package dojo.shop.orders;

import dojo.shop.billing.Invoice;
import dojo.shop.billing.InvoiceService;
import java.util.List;

/** Places orders and has them invoiced. */
public class OrderService {
    private final OrderRepository orders;
    private final InvoiceService invoices;

    public OrderService(OrderRepository orders, InvoiceService invoices) {
        this.orders = orders;
        this.invoices = invoices;
    }

    public Order place(String id, String customer, List<OrderLine> lines) {
        if (orders.find(id).isPresent()) {
            throw new IllegalArgumentException("duplicate order " + id);
        }
        Order order = new Order(id, customer, lines);
        orders.save(order);
        Invoice invoice = invoices.invoiceFor(order);
        order.markInvoiced(invoice.id());
        return order;
    }
}
