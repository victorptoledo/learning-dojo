package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.events.Broker;
import dojo.events.Database;
import dojo.events.Event;
import dojo.events.OrderRow;
import dojo.events.OrderService;
import dojo.events.OutboxMessage;
import dojo.events.OutboxRelay;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxGradingTest {
    /** A broker the test can take down. */
    static final class FakeBroker implements Broker {
        final List<Event> received = new ArrayList<>();
        int failAfter = Integer.MAX_VALUE;

        @Override
        public void publish(Event event) {
            if (received.size() >= failAfter) {
                throw new IllegalStateException("broker down");
            }
            received.add(event);
        }
    }

    private final Database db = new Database();
    private final FakeBroker broker = new FakeBroker();
    private final OrderService orders = new OrderService(db);
    private final OutboxRelay relay = new OutboxRelay(db, broker);

    private static Event placed(String order, String customer, long cents) {
        return new Event("evt-" + order + "-placed", "OrderPlaced", order, customer, cents);
    }

    @Test
    void theServiceDoesNotKnowTheBroker() {
        assertFalse(java.util.Arrays.stream(OrderService.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == Broker.class), "OrderService should not hold a Broker");
    }

    @Test
    void aPlacedOrderWritesTheRowAndTheMessageTogether() {
        orders.place("o-1", "ada", 2500);
        assertEquals(new OrderRow("o-1", "ada", 2500, "PLACED"), db.orders().get("o-1"));
        assertEquals(List.of(new OutboxMessage(placed("o-1", "ada", 2500), false)), db.outbox());
        assertEquals(List.of(), broker.received);
    }

    @Test
    void refusedOrdersWriteNothing() {
        assertThrows(IllegalArgumentException.class, () -> orders.place("o-1", "ada", 0));
        assertTrue(db.orders().isEmpty());
        assertEquals(List.of(), db.outbox());
        orders.place("o-2", "ada", 100);
        assertThrows(IllegalArgumentException.class, () -> orders.place("o-2", "bob", 100));
        assertEquals(1, db.outbox().size());
        assertEquals(1, relay.relay());
        assertEquals(List.of(placed("o-2", "ada", 100)), broker.received);
    }

    @Test
    void cancellingWritesTheStatusAndTheMessageTogether() {
        orders.place("o-1", "ada", 2500);
        orders.cancel("o-1");
        assertEquals("CANCELLED", db.orders().get("o-1").status());
        assertEquals(new OutboxMessage(new Event("evt-o-1-cancelled", "OrderCancelled", "o-1", "ada", 2500), false),
                db.outbox().get(1));
        assertThrows(IllegalStateException.class, () -> orders.cancel("o-1"));
        assertThrows(IllegalArgumentException.class, () -> orders.cancel("o-9"));
        assertEquals(2, db.outbox().size());
    }

    @Test
    void theRelayPublishesInOrderAndMarksMessagesSent() {
        orders.place("o-1", "ada", 100);
        orders.place("o-2", "bob", 200);
        assertEquals(2, relay.relay());
        assertEquals(List.of(placed("o-1", "ada", 100), placed("o-2", "bob", 200)), broker.received);
        assertTrue(db.outbox().stream().allMatch(OutboxMessage::sent));
        assertEquals(0, relay.relay());
        assertEquals(2, broker.received.size());
    }

    @Test
    void aBrokerThatFailsHalfWayIsResumedLater() {
        orders.place("o-1", "ada", 100);
        orders.place("o-2", "bob", 200);
        orders.place("o-3", "cy", 300);
        broker.failAfter = 1;

        assertEquals(1, relay.relay());
        assertEquals(List.of(true, false, false), db.outbox().stream().map(OutboxMessage::sent).toList());

        broker.failAfter = Integer.MAX_VALUE;
        orders.place("o-4", "dee", 400);
        assertEquals(3, relay.relay());
        assertEquals(List.of("o-1", "o-2", "o-3", "o-4"), broker.received.stream().map(Event::orderId).toList());
    }
}
