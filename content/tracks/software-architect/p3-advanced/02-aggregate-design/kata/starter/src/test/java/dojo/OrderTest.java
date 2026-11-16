package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.orders.CustomerId;
import dojo.orders.Money;
import dojo.orders.Order;
import dojo.orders.OrderId;
import dojo.orders.OrderSubmitted;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {
    @Test
    void aSubmittedOrderRecordsAnEventAndAcceptsNoMoreLines() {
        Order order = Order.draft(new OrderId("o-1"), new CustomerId("c-1"), "EUR");
        order.addLine("pen", 2, new Money(150, "EUR"));
        order.addLine("pen", 1, new Money(150, "EUR"));
        order.submit();

        assertEquals(new Money(450, "EUR"), order.total());
        assertEquals(List.of(new OrderSubmitted(new OrderId("o-1"), new CustomerId("c-1"), new Money(450, "EUR"))),
                order.pullEvents());
        assertThrows(IllegalStateException.class, () -> order.addLine("ink", 1, new Money(400, "EUR")));
    }
}
