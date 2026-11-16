package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.orders.Customer;
import dojo.orders.CustomerId;
import dojo.orders.DomainEvent;
import dojo.orders.Money;
import dojo.orders.Order;
import dojo.orders.OrderId;
import dojo.orders.OrderLine;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AggregateStructureGradingTest {
    @Test
    void orderIsAClosedRoot() {
        assertTrue(Modifier.isFinal(Order.class.getModifiers()), "Order should be final");
        assertEquals(0, Order.class.getConstructors().length, "Order should have no public constructor");
        List<String> setters = Arrays.stream(Order.class.getMethods())
                .map(Method::getName).filter(name -> name.startsWith("set")).toList();
        assertEquals(List.of(), setters, "Order should have no setters");
    }

    @Test
    void orderReferencesTheCustomerById() {
        assertFalse(Arrays.stream(Order.class.getDeclaredFields()).anyMatch(field -> field.getType() == Customer.class),
                "Order should hold a CustomerId, not a Customer");
    }

    @Test
    void theValueObjectsAreRecordsAndTheEventsSealed() {
        for (Class<?> type : List.of(Money.class, OrderId.class, CustomerId.class, OrderLine.class)) {
            assertTrue(type.isRecord(), type.getSimpleName() + " should be a record");
        }
        assertTrue(DomainEvent.class.isSealed(), "DomainEvent should be sealed");
    }

    @Test
    void linesCannotBeChangedFromOutside() {
        Order order = Order.draft(new OrderId("o-1"), new CustomerId("c-1"), "EUR");
        order.addLine("pen", 1, new Money(100, "EUR"));
        List<OrderLine> lines = order.lines();
        assertThrows(UnsupportedOperationException.class, () -> lines.add(new OrderLine("x", 1, new Money(1, "EUR"))));
        order.addLine("ink", 1, new Money(100, "EUR"));
        assertEquals(1, lines.size(), "lines() should return a copy");
    }

    @Test
    void valueObjectsValidateThemselves() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-1, "EUR"));
        assertThrows(IllegalArgumentException.class, () -> new Money(1, "EURO"));
        assertThrows(IllegalArgumentException.class, () -> new OrderId(" "));
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(""));
        assertThrows(IllegalArgumentException.class, () -> new Money(1, "EUR").plus(new Money(1, "USD")));
        assertEquals(new Money(7, "EUR"), new Money(3, "EUR").plus(new Money(4, "EUR")));
        assertEquals(new Money(12, "EUR"), new Money(3, "EUR").times(4));
        assertEquals(new Money(0, "BRL"), Money.zero("BRL"));
    }
}
