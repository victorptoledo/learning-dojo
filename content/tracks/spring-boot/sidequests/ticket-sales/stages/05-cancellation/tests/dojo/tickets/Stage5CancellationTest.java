package dojo.tickets;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class Stage5CancellationTest extends TicketsContractTest {

    static String key() {
        return "key-" + UUID.randomUUID();
    }

    ResultActions purchase(RequestPostProcessor who, long hold, String key) throws Exception {
        return mvc.perform(post("/holds/" + hold + "/purchase").with(who).header("Idempotency-Key", key));
    }

    long bought(RequestPostProcessor who, long event, String key, String... seats) throws Exception {
        long hold = held(who, event, seats);
        return idOf(purchase(who, hold, key).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString());
    }

    ResultActions cancel(RequestPostProcessor who, long event) throws Exception {
        return mvc.perform(delete("/events/" + event).with(who));
    }

    ResultActions report(long event) throws Exception {
        return mvc.perform(get("/events/" + event + "/report").with(BOSS));
    }

    @Test
    void a_cancellation_refunds_orders_and_releases_holds() throws Exception {
        long event = event(1, 4, 1000);
        String key = key();
        long hold = held(ADA, event, "A1", "A2");
        long order = idOf(purchase(ADA, hold, key).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString());
        long linusHold = held(LINUS, event, "A3");
        mvc.perform(get("/events/" + event).with(ADA)).andExpect(jsonPath("$.status").value("ON_SALE"));

        cancel(BOSS, event)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundedOrders").value(1))
                .andExpect(jsonPath("$.refundedCents").value(2000))
                .andExpect(jsonPath("$.releasedHolds").value(1));

        mvc.perform(get("/orders/" + order).with(ADA)).andExpect(jsonPath("$.status").value("REFUNDED"));
        mvc.perform(get("/holds/" + linusHold).with(LINUS)).andExpect(jsonPath("$.status").value("RELEASED"));
        mvc.perform(get("/events/" + event).with(ADA))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.available").value(0));
        mvc.perform(get("/events/" + event + "/seats").with(ADA))
                .andExpect(jsonPath("$[*].status", everyItem(is("AVAILABLE"))));
    }

    @Test
    void nothing_is_sold_after_a_cancellation() throws Exception {
        long event = event(1, 4, 1000);
        String key = key();
        long hold = held(ADA, event, "A1");
        long order = idOf(purchase(ADA, hold, key).andReturn().getResponse().getContentAsString());
        long pending = held(GRACE, event, "A2");
        cancel(BOSS, event).andExpect(status().isOk());

        String detail = "event " + event + " is cancelled";
        hold(LINUS, event, "A4").andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(detail));
        purchase(GRACE, pending, key()).andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(detail));
        purchase(ADA, hold, key).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        cancel(BOSS, event).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("event " + event + " is already cancelled"));
    }

    @Test
    void only_the_organizer_cancels_and_reads_reports() throws Exception {
        long event = event(1, 1, 100);
        cancel(ADA, event).andExpect(status().isForbidden());
        mvc.perform(get("/events/" + event + "/report").with(ADA)).andExpect(status().isForbidden());
        cancel(BOSS, 987654).andExpect(status().isNotFound());
        mvc.perform(get("/events/987654/report").with(BOSS)).andExpect(status().isNotFound());
        mvc.perform(get("/events/" + event).with(ADA)).andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void the_report_adds_up_the_sale() throws Exception {
        long event = event(2, 5, 500);
        held(GRACE, event, "B5");
        clock.advance(Duration.ofMinutes(7));
        bought(ADA, event, key(), "A1", "A2");
        held(LINUS, event, "B1", "B2", "B3");

        report(event)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_SALE"))
                .andExpect(jsonPath("$.seats").value(10))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.held").value(3))
                .andExpect(jsonPath("$.sold").value(2))
                .andExpect(jsonPath("$.paidOrders").value(1))
                .andExpect(jsonPath("$.revenueCents").value(1000))
                .andExpect(jsonPath("$.refundedOrders").value(0))
                .andExpect(jsonPath("$.refundedCents").value(0));

        cancel(BOSS, event).andExpect(status().isOk()).andExpect(jsonPath("$.releasedHolds").value(1));
        report(event)
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.available").value(0))
                .andExpect(jsonPath("$.held").value(0))
                .andExpect(jsonPath("$.sold").value(0))
                .andExpect(jsonPath("$.paidOrders").value(0))
                .andExpect(jsonPath("$.revenueCents").value(0))
                .andExpect(jsonPath("$.refundedOrders").value(1))
                .andExpect(jsonPath("$.refundedCents").value(1000));
    }

    @Test
    void purchases_in_flight_are_refunded_or_refused() throws Exception {
        long event = event(1, 12, 300);
        List<Long> holds = new ArrayList<>();
        for (int seat = 1; seat <= 12; seat++) {
            holds.add(held(ADA, event, "A" + seat));
        }
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < holds.size(); i++) {
            long hold = holds.get(i);
            String key = key();
            calls.add(() -> send("ada", "/holds/" + hold + "/purchase", "", "Idempotency-Key", key));
            if (i == holds.size() / 2) {
                calls.add(() -> sendDelete("boss", "/events/" + event));
            }
        }
        List<Reply> replies = together(calls);
        Reply cancelled = replies.get(holds.size() / 2 + 1);
        assertEquals(200, cancelled.status(), "the cancellation answered " + cancelled.status() + " " + cancelled.body());
        List<Reply> purchases = new ArrayList<>(replies);
        purchases.remove(cancelled);
        assertEquals(12, count(purchases, 201) + count(purchases, 409),
                "statuses: " + purchases.stream().map(Reply::status).toList());

        long paid = count(purchases, 201);
        assertEquals(paid, ((Number) JsonPath.read(cancelled.body(), "$.refundedOrders")).longValue(),
                "every purchase that answered 201 must be refunded by the cancellation");
        report(event)
                .andExpect(jsonPath("$.paidOrders").value(0))
                .andExpect(jsonPath("$.refundedOrders").value(paid))
                .andExpect(jsonPath("$.refundedCents").value(paid * 300))
                .andExpect(jsonPath("$.revenueCents").value(0));
    }
}
