package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentsGradingTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    FakeGateway gateway;

    ResultActions pay(String key, String account, long cents) throws Exception {
        var request = post("/payments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\": \"" + account + "\", \"amountCents\": " + cents + "}");
        if (key != null) {
            request = request.header("Idempotency-Key", key);
        }
        return mvc.perform(request);
    }

    @Test
    void a_first_request_charges_once_and_creates_the_payment() throws Exception {
        int before = gateway.charges();
        pay("key-first", "acct-1", 2500)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andExpect(jsonPath("$.amountCents").value(2500))
                .andExpect(jsonPath("$.chargeId").exists());
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void a_retry_with_the_same_key_replays_without_charging_again() throws Exception {
        String first = pay("key-retry", "acct-2", 4000).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int charges = gateway.charges();
        String second = pay("key-retry", "acct-2", 4000)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(charges, gateway.charges(), "the retry must not charge");
        assertEquals((Object) JsonPath.read(first, "$.id"), JsonPath.read(second, "$.id"));
        assertEquals((Object) JsonPath.read(first, "$.chargeId"), JsonPath.read(second, "$.chargeId"));
    }

    @Test
    void reusing_a_key_for_another_request_is_422() throws Exception {
        pay("key-reuse", "acct-3", 1000).andExpect(status().isCreated());
        int charges = gateway.charges();
        pay("key-reuse", "acct-3", 9999)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("this Idempotency-Key was used for a different request"));
        assertEquals(charges, gateway.charges());
    }

    @Test
    void different_keys_are_different_payments() throws Exception {
        String a = pay("key-a", "acct-4", 500).andReturn().getResponse().getContentAsString();
        String b = pay("key-b", "acct-4", 500).andReturn().getResponse().getContentAsString();
        assertTrue(!JsonPath.read(a, "$.id").equals(JsonPath.read(b, "$.id")));
    }

    @Test
    void a_declined_charge_is_402_and_replays_as_402_without_charging() throws Exception {
        int before = gateway.charges();
        pay("key-declined", "acct-5", 1013).andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.detail").value("card declined for acct-5"));
        pay("key-declined", "acct-5", 1013).andExpect(status().isPaymentRequired());
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void the_key_is_required_and_the_body_validated() throws Exception {
        pay(null, "acct-6", 100).andExpect(status().isBadRequest());
        pay("key-invalid", " ", 100).andExpect(status().isBadRequest());
        pay("key-invalid-2", "acct-6", -5).andExpect(status().isBadRequest());
    }

    @Test
    void concurrent_requests_with_one_key_charge_exactly_once() throws Exception {
        int before = gateway.charges();
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        try (var executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    statuses.add(pay("key-race", "acct-7", 7700).andReturn().getResponse().getStatus());
                    return null;
                });
            }
        }
        assertEquals(before + 1, gateway.charges(), "statuses: " + statuses);
        assertEquals(10, statuses.size());
        assertTrue(statuses.stream().allMatch(s -> s == 201 || s == 409), "statuses: " + statuses);
        assertTrue(statuses.contains(201));
        pay("key-race", "acct-7", 7700).andExpect(status().isCreated()).andExpect(header().string("Idempotent-Replayed", "true"));
    }
}
