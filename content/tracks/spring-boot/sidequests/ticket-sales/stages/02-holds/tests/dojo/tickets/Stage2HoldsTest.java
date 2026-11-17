package dojo.tickets;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class Stage2HoldsTest extends TicketsContractTest {
    static final String[] BUYERS = {"ada", "linus", "grace"};

    long available(long event) throws Exception {
        String body = mvc.perform(get("/events/" + event).with(ADA)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.available")).longValue();
    }

    List<String> seatsWith(long event, String status) throws Exception {
        String body = mvc.perform(get("/events/" + event + "/seats").with(ADA)).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[?(@.status == '" + status + "')].seat");
    }

    @Test
    void a_hold_keeps_seats_for_a_buyer() throws Exception {
        clock.advance(Duration.ofSeconds(5));
        Instant now = clock.instant();
        long event = event(3, 10, 4500);
        String body = hold(ADA, event, "B10", "A2", "B9")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.eventId").value(event))
                .andExpect(jsonPath("$.seats", contains("A2", "B9", "B10")))
                .andExpect(jsonPath("$.totalCents").value(13500))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andReturn().getResponse().getContentAsString();
        String expiresAt = JsonPath.read(body, "$.expiresAt");
        assertEquals(now.plus(Duration.ofMinutes(7)), Instant.parse(expiresAt), "expiresAt = now + tickets.hold-ttl");

        long id = idOf(body);
        mvc.perform(get("/holds/" + id).with(ADA)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HELD"));
        assertEquals(27, available(event));
        assertEquals(List.of("A2", "B9", "B10"), seatsWith(event, "HELD"));
    }

    @Test
    void a_bad_hold_is_400() throws Exception {
        long event = event(1, 10, 100);
        hold(ADA, event).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.seats").isString());
        hold(ADA, event, "A1", "A2", "A3", "A4", "A5").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.seats").isString());
        hold(ADA, event, "A1", "A1").andExpect(status().isBadRequest());
        hold(ADA, event, "A1", "A2", "A3", "A4").andExpect(status().isCreated());
    }

    @Test
    void seats_and_events_must_exist() throws Exception {
        long event = event(1, 3, 100);
        hold(ADA, event, "A1", "Z9").andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("unknown seat Z9"));
        hold(ADA, 987654, "A1").andExpect(status().isNotFound());
        assertEquals(3, available(event));
    }

    @Test
    void a_hold_is_all_or_nothing() throws Exception {
        long event = event(1, 5, 100);
        held(LINUS, event, "A2", "A4");
        hold(ADA, event, "A1", "A4", "A2")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.unavailable", contains("A2", "A4")));
        assertEquals(List.of("A1", "A3", "A5"), seatsWith(event, "AVAILABLE"));
        held(ADA, event, "A1", "A3");
    }

    @Test
    void holds_belong_to_their_buyer() throws Exception {
        long event = event(1, 3, 100);
        long id = held(ADA, event, "A1");
        mvc.perform(get("/holds/" + id).with(LINUS)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("hold " + id + " not found"));
        mvc.perform(delete("/holds/" + id).with(LINUS)).andExpect(status().isNotFound());
        mvc.perform(get("/holds/987654").with(ADA)).andExpect(status().isNotFound());
        mvc.perform(post("/events/" + event + "/holds").with(BOSS).contentType(MediaType.APPLICATION_JSON)
                .content(seatsJson("A2"))).andExpect(status().isForbidden());
    }

    @Test
    void a_released_hold_gives_its_seats_back() throws Exception {
        long event = event(1, 3, 100);
        long id = held(ADA, event, "A1", "A2");
        mvc.perform(delete("/holds/" + id).with(ADA)).andExpect(status().isNoContent());
        mvc.perform(get("/holds/" + id).with(ADA)).andExpect(jsonPath("$.status").value("RELEASED"));
        mvc.perform(delete("/holds/" + id).with(ADA)).andExpect(status().isConflict());
        assertEquals(3, available(event));
        held(LINUS, event, "A1", "A2");
    }

    @Test
    void two_dozen_buyers_one_seat_one_winner() throws Exception {
        long event = event(5, 5, 100);
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            String buyer = BUYERS[i % BUYERS.length];
            calls.add(() -> send(buyer, "/events/" + event + "/holds", seatsJson("C3")));
        }
        List<Reply> replies = together(calls);
        assertEquals(1, count(replies, 201), "statuses: " + replies.stream().map(Reply::status).toList());
        assertEquals(23, count(replies, 409), "statuses: " + replies.stream().map(Reply::status).toList());
        assertEquals(24, available(event));
    }

    @Test
    void overlapping_holds_never_share_a_seat() throws Exception {
        long event = event(1, 8, 100);
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            int first = 1 + i % 7;
            String buyer = BUYERS[i % BUYERS.length];
            calls.add(() -> send(buyer, "/events/" + event + "/holds", seatsJson("A" + first, "A" + (first + 1))));
        }
        List<Reply> replies = together(calls);
        assertEquals(24, count(replies, 201) + count(replies, 409),
                "statuses: " + replies.stream().map(Reply::status).toList());
        Set<String> won = new HashSet<>();
        for (Reply reply : replies) {
            if (reply.status() == 201) {
                List<String> seats = JsonPath.read(reply.body(), "$.seats");
                for (String seat : seats) {
                    assertTrue(won.add(seat), "seat " + seat + " was held twice");
                }
            }
        }
        assertTrue(won.size() >= 2, "no hold won at all");
        assertEquals(won, new HashSet<>(seatsWith(event, "HELD")));
        assertEquals(8 - won.size(), available(event));
    }
}
