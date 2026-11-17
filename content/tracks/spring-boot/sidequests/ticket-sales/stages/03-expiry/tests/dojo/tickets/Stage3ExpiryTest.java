package dojo.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class Stage3ExpiryTest extends TicketsContractTest {
    static final String[] BUYERS = {"ada", "linus", "grace"};

    long available(long event) throws Exception {
        String body = mvc.perform(get("/events/" + event).with(ADA)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.available")).longValue();
    }

    String statusOf(long event, String seat) throws Exception {
        String body = mvc.perform(get("/events/" + event + "/seats").with(ADA)).andReturn().getResponse()
                .getContentAsString();
        List<String> found = JsonPath.read(body, "$[?(@.seat == '" + seat + "')].status");
        return found.get(0);
    }

    @Test
    void a_hold_expires_after_the_configured_time() throws Exception {
        long event = event(1, 4, 100);
        long id = held(ADA, event, "A1", "A2");

        clock.advance(Duration.ofMinutes(7).minusSeconds(1));
        mvc.perform(get("/holds/" + id).with(ADA)).andExpect(jsonPath("$.status").value("HELD"));
        assertEquals("HELD", statusOf(event, "A1"));
        assertEquals(2, available(event));
        hold(LINUS, event, "A1").andExpect(status().isConflict());

        clock.advance(Duration.ofSeconds(1));
        mvc.perform(get("/holds/" + id).with(ADA)).andExpect(jsonPath("$.status").value("EXPIRED"));
        assertEquals("AVAILABLE", statusOf(event, "A1"));
        assertEquals(4, available(event));
        mvc.perform(delete("/holds/" + id).with(ADA)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("hold " + id + " is not active"));
        held(LINUS, event, "A1", "A2");
    }

    @Test
    void an_expired_hold_never_comes_back() throws Exception {
        long event = event(1, 4, 100);
        long id = held(ADA, event, "A1", "A2");
        clock.advance(Duration.ofMinutes(8));
        held(LINUS, event, "A2");
        assertEquals("AVAILABLE", statusOf(event, "A1"));
        assertEquals("HELD", statusOf(event, "A2"));
        mvc.perform(get("/holds/" + id).with(ADA)).andExpect(jsonPath("$.status").value("EXPIRED"));
        assertEquals(3, available(event));
        held(GRACE, event, "A1");
    }

    @Test
    void the_new_hold_expires_on_its_own_time() throws Exception {
        long event = event(1, 2, 100);
        held(ADA, event, "A1");
        clock.advance(Duration.ofMinutes(7));
        long second = held(LINUS, event, "A1");
        clock.advance(Duration.ofMinutes(3));
        hold(GRACE, event, "A1").andExpect(status().isConflict());
        mvc.perform(get("/holds/" + second).with(LINUS)).andExpect(jsonPath("$.status").value("HELD"));
        clock.advance(Duration.ofMinutes(4));
        held(GRACE, event, "A1");
    }

    @Test
    void a_seat_freed_by_expiry_still_has_one_winner() throws Exception {
        long event = event(2, 2, 100);
        held(ADA, event, "B2");
        clock.advance(Duration.ofMinutes(10));
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            String buyer = BUYERS[i % BUYERS.length];
            calls.add(() -> send(buyer, "/events/" + event + "/holds", seatsJson("B2")));
        }
        List<Reply> replies = together(calls);
        assertEquals(1, count(replies, 201), "statuses: " + replies.stream().map(Reply::status).toList());
        assertEquals(23, count(replies, 409), "statuses: " + replies.stream().map(Reply::status).toList());
        assertEquals(3, available(event));
    }
}
