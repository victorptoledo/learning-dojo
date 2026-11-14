package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** Each test starts with an empty database and broker. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OutboxGradingTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeBroker broker;

    @Autowired
    OutboxRelay relay;

    @Autowired
    OutboxRepository outbox;

    @Autowired
    OrderRepository orders;

    long place(String customer, long cents) throws Exception {
        String json = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"" + customer + "\", \"totalCents\": " + cents + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Autowired
    ShippingConsumer shipping;

    @Autowired
    ShipmentRepository shipments;

    @BeforeEach
    void cleanBroker() {
        broker.clear();
    }

    @Test
    void placing_an_order_writes_an_outbox_event_and_publishes_nothing_yet() throws Exception {
        long id = place("ada", 500);
        var events = outbox.findAll();
        assertEquals(1, events.size());
        var event = events.getFirst();
        assertEquals("OrderPlaced", event.getType());
        assertEquals("{\"orderId\":" + id + ",\"customer\":\"ada\",\"totalCents\":500}", event.getPayload());
        assertEquals(null, event.getPublishedAt());
        assertTrue(broker.published().isEmpty(), "publishing is the relay's job, after the commit");
    }

    @Test
    void a_failed_order_leaves_neither_the_order_nor_its_event() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"customer\": \"blocked\", \"totalCents\": 100}"))
                .andExpect(status().isUnprocessableContent());
        assertEquals(0, orders.countByCustomer("blocked"));
        assertEquals(0, outbox.count());
    }

    @Test
    void the_relay_publishes_pending_events_in_order_and_marks_them() throws Exception {
        long first = place("a", 100);
        long second = place("b", 200);
        long third = place("c", 300);
        assertEquals(new RelayResult(2, false), relay.publishPending(2));
        assertEquals(new RelayResult(1, false), relay.publishPending(10));
        assertEquals(new RelayResult(0, false), relay.publishPending(10));
        var published = broker.published();
        assertEquals(3, published.size());
        assertTrue(published.get(0).payload().contains("\"orderId\":" + first));
        assertTrue(published.get(1).payload().contains("\"orderId\":" + second));
        assertTrue(published.get(2).payload().contains("\"orderId\":" + third));
        assertTrue(outbox.findAll().stream().allMatch(e -> e.getPublishedAt() != null));
        assertEquals(outbox.findAll().getFirst().getEventId(), published.getFirst().eventId());
    }

    @Test
    void a_broker_failure_stops_the_run_keeps_the_order_and_retries_later() throws Exception {
        place("x", 1);
        place("y", 2);
        broker.failNext(1);
        assertEquals(new RelayResult(0, true), relay.publishPending(10));
        assertEquals(1, outbox.findAll().getFirst().getAttempts());
        assertTrue(broker.published().isEmpty(), "the second event must not overtake the first");
        assertEquals(new RelayResult(2, false), relay.publishPending(10));
        assertTrue(broker.published().get(0).payload().contains("\"customer\":\"x\""));
    }

    @Test
    void an_event_that_keeps_failing_is_parked_as_dead() throws Exception {
        place("poison", 9);
        place("next", 10);
        broker.failNext(OutboxEvent.MAX_ATTEMPTS);
        for (int i = 0; i < OutboxEvent.MAX_ATTEMPTS; i++) {
            assertTrue(relay.publishPending(10).failed());
        }
        assertTrue(outbox.findAll().getFirst().isDead());
        assertEquals(new RelayResult(1, false), relay.publishPending(10), "the queue moves on past the dead event");
        assertTrue(broker.published().getFirst().payload().contains("\"customer\":\"next\""));
    }

    @Test
    void the_consumer_ships_each_order_once_even_if_the_event_comes_twice() throws Exception {
        long id = place("dup", 700);
        relay.publishPending(10);
        var message = broker.published().getFirst();
        assertTrue(shipping.on(message.eventId(), message.type(), message.payload()));
        assertFalse(shipping.on(message.eventId(), message.type(), message.payload()));
        assertEquals(1, shipments.countByOrderId(id));
    }

    @Test
    void concurrent_duplicate_deliveries_still_ship_once() throws Exception {
        long id = place("race", 900);
        relay.publishPending(10);
        var message = broker.published().getFirst();
        var processedNow = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 8; i++) {
                executor.submit(() -> {
                    if (shipping.on(message.eventId(), message.type(), message.payload())) {
                        processedNow.incrementAndGet();
                    }
                });
            }
        }
        assertEquals(1, processedNow.get());
        assertEquals(1, shipments.countByOrderId(id));
    }
}
