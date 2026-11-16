package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dojo.events.CustomerSummary;
import dojo.events.Database;
import dojo.events.Event;
import dojo.events.OrderService;
import dojo.events.OrderSummaryProjection;
import dojo.events.OutboxRelay;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxTest {
    @Test
    void aPlacedOrderReachesTheReadModelThroughTheRelay() {
        Database db = new Database();
        List<Event> published = new ArrayList<>();
        OrderSummaryProjection projection = new OrderSummaryProjection();
        OrderService orders = new OrderService(db);
        OutboxRelay relay = new OutboxRelay(db, event -> {
            published.add(event);
            projection.accept(event);
        });

        orders.place("o-1", "ada", 2500);
        assertEquals(List.of(), published);

        assertEquals(1, relay.relay());
        assertEquals(new CustomerSummary("ada", 1, 2500), projection.summary("ada"));
    }
}
