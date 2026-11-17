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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class Stage4PurchaseTest extends TicketsContractTest {

    static String key() {
        return "key-" + UUID.randomUUID();
    }

    ResultActions purchase(RequestPostProcessor who, long hold, String key) throws Exception {
        return mvc.perform(post("/holds/" + hold + "/purchase").with(who).header("Idempotency-Key", key));
    }

    List<String> seatsWith(long event, String status) throws Exception {
        String body = mvc.perform(get("/events/" + event + "/seats").with(ADA)).andReturn().getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[?(@.status == '" + status + "')].seat");
    }

    @Test
    void a_purchase_turns_a_hold_into_an_order() throws Exception {
        long event = event(1, 4, 2500);
        long hold = held(ADA, event, "A3", "A1");
        String key = key();
        String location = purchase(ADA, hold, key)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.holdId").value(hold))
                .andExpect(jsonPath("$.eventId").value(event))
                .andExpect(jsonPath("$.seats", contains("A1", "A3")))
                .andExpect(jsonPath("$.totalCents").value(5000))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andReturn().getResponse().getHeader("Location");
        assertTrue(location != null && location.matches(".*/orders/\\d+"), "Location was " + location);
        mvc.perform(get(location).with(ADA)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/holds/" + hold).with(ADA)).andExpect(jsonPath("$.status").value("PURCHASED"));
        assertEquals(List.of("A1", "A3"), seatsWith(event, "SOLD"));

        clock.advance(Duration.ofMinutes(30));
        assertEquals(List.of("A1", "A3"), seatsWith(event, "SOLD"));
        mvc.perform(get("/holds/" + hold).with(ADA)).andExpect(jsonPath("$.status").value("PURCHASED"));
        hold(LINUS, event, "A1").andExpect(status().isConflict());
    }

    @Test
    void a_retry_with_the_same_key_gets_the_same_order() throws Exception {
        long event = event(1, 2, 100);
        long hold = held(ADA, event, "A1");
        String key = key();
        long order = idOf(purchase(ADA, hold, key).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString());
        purchase(ADA, hold, key).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(order));
        purchase(ADA, hold, key()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("hold " + hold + " is already purchased"));
        long other = held(ADA, event, "A2");
        purchase(ADA, other, key).andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("idempotency key already used for another purchase"));
    }

    @Test
    void keys_belong_to_their_buyer() throws Exception {
        long event = event(1, 2, 100);
        String key = key();
        purchase(ADA, held(ADA, event, "A1"), key).andExpect(status().isCreated());
        purchase(LINUS, held(LINUS, event, "A2"), key).andExpect(status().isCreated());
    }

    @Test
    void the_key_is_required_and_checked() throws Exception {
        long event = event(1, 2, 100);
        long hold = held(ADA, event, "A1");
        mvc.perform(post("/holds/" + hold + "/purchase").with(ADA))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        purchase(ADA, hold, "short").andExpect(status().isBadRequest());
        purchase(ADA, hold, "x".repeat(65)).andExpect(status().isBadRequest());
        purchase(ADA, hold, "spaces are bad").andExpect(status().isBadRequest());
        mvc.perform(get("/holds/" + hold).with(ADA)).andExpect(jsonPath("$.status").value("HELD"));
    }

    @Test
    void only_an_active_hold_of_the_buyer_can_be_bought() throws Exception {
        long event = event(1, 4, 100);
        long hold = held(ADA, event, "A1");
        purchase(LINUS, hold, key()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("hold " + hold + " not found"));

        long released = held(ADA, event, "A2");
        mvc.perform(delete("/holds/" + released).with(ADA)).andExpect(status().isNoContent());
        purchase(ADA, released, key()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("hold " + released + " is not active"));

        clock.advance(Duration.ofMinutes(7));
        purchase(ADA, hold, key()).andExpect(status().isGone())
                .andExpect(jsonPath("$.detail").value("hold " + hold + " has expired"));
        assertEquals(List.of(), seatsWith(event, "SOLD"));
    }

    @Test
    void orders_belong_to_their_buyer() throws Exception {
        long event = event(1, 1, 100);
        long order = idOf(purchase(GRACE, held(GRACE, event, "A1"), key()).andReturn().getResponse()
                .getContentAsString());
        mvc.perform(get("/orders/" + order).with(ADA)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("order " + order + " not found"));
        mvc.perform(get("/orders/987654").with(GRACE)).andExpect(status().isNotFound());
    }

    @Test
    void a_dozen_retries_at_once_make_one_order() throws Exception {
        long event = event(1, 3, 100);
        long hold = held(ADA, event, "A2");
        String key = key();
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            calls.add(() -> send("ada", "/holds/" + hold + "/purchase", "", "Idempotency-Key", key));
        }
        List<Reply> replies = together(calls);
        List<Integer> statuses = replies.stream().map(Reply::status).toList();
        assertEquals(1, count(replies, 201), "statuses: " + statuses);
        assertEquals(11, count(replies, 200), "statuses: " + statuses);
        Set<Long> orders = new HashSet<>();
        replies.forEach(reply -> orders.add(idOf(reply.body())));
        assertEquals(1, orders.size(), "different orders: " + orders);
    }

    @Test
    void a_dozen_different_purchases_at_once_make_one_order() throws Exception {
        long event = event(1, 3, 100);
        long hold = held(LINUS, event, "A3");
        List<Callable<Reply>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String key = key();
            calls.add(() -> send("linus", "/holds/" + hold + "/purchase", "", "Idempotency-Key", key));
        }
        List<Reply> replies = together(calls);
        List<Integer> statuses = replies.stream().map(Reply::status).toList();
        assertEquals(1, count(replies, 201), "statuses: " + statuses);
        assertEquals(11, count(replies, 409), "statuses: " + statuses);
    }
}
