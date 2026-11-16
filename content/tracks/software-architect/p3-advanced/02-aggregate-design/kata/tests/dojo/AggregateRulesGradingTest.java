package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dojo.orders.CustomerId;
import dojo.orders.DomainEvent;
import dojo.orders.Money;
import dojo.orders.Order;
import dojo.orders.OrderCancelled;
import dojo.orders.OrderId;
import dojo.orders.OrderLine;
import dojo.orders.OrderRepository;
import dojo.orders.OrderService;
import dojo.orders.OrderStatus;
import dojo.orders.OrderSubmitted;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AggregateRulesGradingTest {
    private static Money eur(long cents) {
        return new Money(cents, "EUR");
    }

    private final Order order = Order.draft(new OrderId("o-1"), new CustomerId("c-1"), "EUR");

    private void assertUnchanged(List<OrderLine> lines, OrderStatus status) {
        assertEquals(lines, order.lines());
        assertEquals(status, order.status());
    }

    @Test
    void anEmptyDraftTotalsZero() {
        assertEquals(eur(0), order.total());
        assertEquals(OrderStatus.DRAFT, order.status());
        assertEquals(List.of(), order.pullEvents());
    }

    @Test
    void theSameSkuAddsUpAtTheSamePriceOnly() {
        order.addLine("pen", 2, eur(150));
        order.addLine("pen", 3, eur(150));
        assertEquals(List.of(new OrderLine("pen", 5, eur(150))), order.lines());
        assertEquals("price changed for pen",
                assertThrows(IllegalArgumentException.class, () -> order.addLine("pen", 1, eur(160))).getMessage());
        assertUnchanged(List.of(new OrderLine("pen", 5, eur(150))), OrderStatus.DRAFT);
    }

    @Test
    void badLinesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> order.addLine("pen", 0, eur(150)));
        assertThrows(IllegalArgumentException.class, () -> order.addLine("pen", 1, new Money(150, "USD")));
        assertEquals(List.of(), order.lines());
    }

    @Test
    void atMostTenSkus() {
        for (int i = 0; i < 10; i++) {
            order.addLine("sku-" + i, 1, eur(100));
        }
        List<OrderLine> before = order.lines();
        assertEquals("an order has at most 10 lines",
                assertThrows(IllegalStateException.class, () -> order.addLine("sku-10", 1, eur(100))).getMessage());
        order.addLine("sku-3", 1, eur(100));
        assertEquals(10, order.lines().size());
        assertEquals(eur(1100), order.total());
        assertEquals(before.size(), order.lines().size());
    }

    @Test
    void theTotalLimitIsKeptAndARefusalChangesNothing() {
        order.addLine("car", 1, eur(999_000));
        order.addLine("pen", 10, eur(100));
        List<OrderLine> before = order.lines();
        assertEquals("order total above limit",
                assertThrows(IllegalStateException.class, () -> order.addLine("pen", 1, eur(100))).getMessage());
        assertThrows(IllegalStateException.class, () -> order.addLine("ink", 1, eur(1)));
        assertUnchanged(before, OrderStatus.DRAFT);
        assertEquals(eur(1_000_000), order.total());
    }

    @Test
    void removingLines() {
        order.addLine("pen", 1, eur(100));
        order.addLine("ink", 1, eur(300));
        order.removeLine("pen");
        assertEquals(List.of(new OrderLine("ink", 1, eur(300))), order.lines());
        assertThrows(IllegalArgumentException.class, () -> order.removeLine("pen"));
    }

    @Test
    void submittingRecordsTheTotalAndFreezesTheOrder() {
        assertEquals("an empty order cannot be submitted",
                assertThrows(IllegalStateException.class, order::submit).getMessage());
        order.addLine("pen", 2, eur(100));
        order.submit();
        assertEquals(List.of(new OrderSubmitted(new OrderId("o-1"), new CustomerId("c-1"), eur(200))), order.pullEvents());
        assertEquals(List.of(), order.pullEvents());
        assertThrows(IllegalStateException.class, () -> order.addLine("ink", 1, eur(1)));
        assertThrows(IllegalStateException.class, () -> order.removeLine("pen"));
        assertThrows(IllegalStateException.class, order::submit);
        assertUnchanged(List.of(new OrderLine("pen", 2, eur(100))), OrderStatus.SUBMITTED);
    }

    @Test
    void cancellingOnceFromDraftOrSubmitted() {
        order.addLine("pen", 1, eur(100));
        order.submit();
        order.cancel("changed my mind");
        assertEquals(OrderStatus.CANCELLED, order.status());
        assertEquals(List.of(
                        new OrderSubmitted(new OrderId("o-1"), new CustomerId("c-1"), eur(100)),
                        new OrderCancelled(new OrderId("o-1"), "changed my mind")),
                order.pullEvents());
        assertThrows(IllegalStateException.class, () -> order.cancel("again"));
        assertEquals(List.of(), order.pullEvents());

        Order draft = Order.draft(new OrderId("o-2"), new CustomerId("c-1"), "EUR");
        draft.cancel("never mind");
        assertEquals(OrderStatus.CANCELLED, draft.status());
    }

    @Test
    void theServiceLoadsCallsSavesAndPublishes() {
        OrderRepository orders = new OrderRepository();
        List<DomainEvent> published = new ArrayList<>();
        OrderService service = new OrderService(orders, published::add);

        OrderId id = service.create("o-9", "c-7", "EUR");
        service.addLine(id, "pen", 3, eur(100));
        service.submit(id);
        service.cancel(id, "late");

        assertEquals(List.of(
                        new OrderSubmitted(new OrderId("o-9"), new CustomerId("c-7"), eur(300)),
                        new OrderCancelled(new OrderId("o-9"), "late")),
                published);
        assertEquals(OrderStatus.CANCELLED, orders.find(id).orElseThrow().status());
        assertThrows(IllegalArgumentException.class, () -> service.submit(new OrderId("nope")));
    }
}
