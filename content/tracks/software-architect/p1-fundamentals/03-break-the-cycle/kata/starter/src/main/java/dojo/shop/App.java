package dojo.shop;

import dojo.shop.billing.InvoiceService;
import dojo.shop.billing.PaymentService;
import dojo.shop.orders.OrderRepository;
import dojo.shop.orders.OrderService;

/** The composition root. */
public final class App {
    private App() {}

    public static Shop create() {
        OrderRepository orderRepository = new OrderRepository();
        InvoiceService invoices = new InvoiceService();
        PaymentService payments = new PaymentService(invoices, orderRepository);
        OrderService orders = new OrderService(orderRepository, invoices);
        return new Shop(orders, payments, orderRepository, invoices);
    }
}
