package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.shop.App;
import dojo.shop.Shop;
import dojo.shop.billing.Invoice;
import dojo.shop.billing.InvoiceRequest;
import dojo.shop.billing.InvoiceService;
import dojo.shop.orders.Order;
import dojo.shop.orders.OrderLine;
import dojo.shop.orders.OrderStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class BillingGradingTest {
    @Test
    void invoicesAreNumberedAndTaxedHalfUp() {
        InvoiceService invoices = new InvoiceService();
        assertEquals(new Invoice("INV-1", "a", "x", 1002, 200), invoices.invoice(new InvoiceRequest("a", "x", 1002)));
        assertEquals(new Invoice("INV-2", "b", "y", 1003, 201), invoices.invoice(new InvoiceRequest("b", "y", 1003)));
        assertEquals(new Invoice("INV-3", "c", "z", 1, 0), invoices.invoice(new InvoiceRequest("c", "z", 1)));
        assertEquals(3, invoices.all().size());
    }

    @Test
    void placingAnOrderInvoicesTheSumOfItsLines() {
        Shop shop = App.create();
        Order order = shop.orders().place("o-1", "ada", List.of(
                new OrderLine("pen", 3, 150), new OrderLine("ink", 2, 275)));

        assertEquals(OrderStatus.INVOICED, order.status());
        assertEquals("INV-1", order.invoiceId());
        assertEquals(new Invoice("INV-1", "o-1", "ada", 1000, 200), shop.invoices().find("INV-1").orElseThrow());
    }

    @Test
    void aFullPaymentMarksTheOrderPaid() {
        Shop shop = App.create();
        shop.orders().place("o-1", "ada", List.of(new OrderLine("pen", 1, 500)));
        shop.orders().place("o-2", "bob", List.of(new OrderLine("map", 1, 800)));

        shop.payments().recordPayment("INV-2", 960);

        assertEquals(OrderStatus.INVOICED, shop.orderRepository().find("o-1").orElseThrow().status());
        assertEquals(OrderStatus.PAID, shop.orderRepository().find("o-2").orElseThrow().status());
    }

    @Test
    void aWrongAmountIsRejectedAndChangesNothing() {
        Shop shop = App.create();
        shop.orders().place("o-1", "ada", List.of(new OrderLine("pen", 1, 500)));

        assertThrows(IllegalArgumentException.class, () -> shop.payments().recordPayment("INV-1", 500));
        assertEquals(OrderStatus.INVOICED, shop.orderRepository().find("o-1").orElseThrow().status());
        shop.payments().recordPayment("INV-1", 600);
        assertEquals(OrderStatus.PAID, shop.orderRepository().find("o-1").orElseThrow().status());
    }

    @Test
    void unknownAndRepeatedPaymentsAreRejected() {
        Shop shop = App.create();
        shop.orders().place("o-1", "ada", List.of(new OrderLine("pen", 1, 500)));

        assertThrows(IllegalArgumentException.class, () -> shop.payments().recordPayment("INV-9", 600));
        shop.payments().recordPayment("INV-1", 600);
        assertThrows(IllegalStateException.class, () -> shop.payments().recordPayment("INV-1", 600));
    }

    @Test
    void aDuplicateOrderIsRejectedBeforeInvoicing() {
        Shop shop = App.create();
        shop.orders().place("o-1", "ada", List.of(new OrderLine("pen", 1, 500)));
        assertThrows(IllegalArgumentException.class,
                () -> shop.orders().place("o-1", "ada", List.of(new OrderLine("pen", 1, 500))));
        assertEquals(1, shop.invoices().all().size());
    }
}
