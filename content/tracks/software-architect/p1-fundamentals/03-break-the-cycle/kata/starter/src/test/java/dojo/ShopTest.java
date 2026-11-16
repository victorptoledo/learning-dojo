package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.shop.App;
import dojo.shop.Shop;
import dojo.shop.billing.Invoice;
import dojo.shop.orders.Order;
import dojo.shop.orders.OrderLine;
import dojo.shop.orders.OrderStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopTest {
    @Test
    void anOrderIsInvoicedAndThenPaid() {
        Shop shop = App.create();
        Order order = shop.orders().place("o-1", "ada", List.of(new OrderLine("book", 2, 1000)));
        Invoice invoice = shop.invoices().find(order.invoiceId()).orElseThrow();

        assertEquals(new Invoice("INV-1", "o-1", "ada", 2000, 400), invoice);
        assertEquals(OrderStatus.INVOICED, order.status());

        shop.payments().recordPayment("INV-1", 2400);
        assertEquals(OrderStatus.PAID, shop.orderRepository().find("o-1").orElseThrow().status());
    }
}
