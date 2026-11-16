package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.events.CustomerSummary;
import dojo.events.Database;
import dojo.events.Event;
import dojo.events.OrderService;
import dojo.events.OrderSummaryProjection;
import dojo.events.OutboxRelay;
import org.junit.jupiter.api.Test;

class ProjectionGradingTest {
    private final OrderSummaryProjection projection = new OrderSummaryProjection();

    @Test
    void anUnknownCustomerHasNothingOpen() {
        assertEquals(new CustomerSummary("nobody", 0, 0), projection.summary("nobody"));
    }

    @Test
    void placedAndCancelledOrdersAreCounted() {
        projection.accept(new Event("e1", "OrderPlaced", "o-1", "ada", 100));
        projection.accept(new Event("e2", "OrderPlaced", "o-2", "ada", 250));
        projection.accept(new Event("e3", "OrderPlaced", "o-3", "bob", 900));
        projection.accept(new Event("e4", "OrderCancelled", "o-1", "ada", 100));

        assertEquals(new CustomerSummary("ada", 1, 250), projection.summary("ada"));
        assertEquals(new CustomerSummary("bob", 1, 900), projection.summary("bob"));
    }

    @Test
    void aRedeliveredEventIsAppliedOnce() {
        Event placed = new Event("e1", "OrderPlaced", "o-1", "ada", 100);
        Event cancelled = new Event("e2", "OrderCancelled", "o-1", "ada", 100);
        projection.accept(placed);
        projection.accept(placed);
        assertEquals(new CustomerSummary("ada", 1, 100), projection.summary("ada"));
        projection.accept(cancelled);
        projection.accept(cancelled);
        projection.accept(placed);
        assertEquals(new CustomerSummary("ada", 0, 0), projection.summary("ada"));
    }

    @Test
    void theReadModelSurvivesABrokerThatRedeliversEverything() {
        Database db = new Database();
        OrderService orders = new OrderService(db);
        OutboxRelay relay = new OutboxRelay(db, event -> {
            projection.accept(event);
            projection.accept(event);
        });

        orders.place("o-1", "ada", 100);
        orders.place("o-2", "ada", 200);
        relay.relay();
        orders.cancel("o-2");
        relay.relay();

        assertEquals(new CustomerSummary("ada", 1, 100), projection.summary("ada"));
    }
}
