package dojo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired
    MockMvc mvc;

    @Autowired
    Gateway gateway;

    @Autowired
    Ticker ticker;

    static String newKey() {
        return UUID.randomUUID().toString();
    }

    ResultActions call(String path, String client, String key, String json) throws Exception {
        var request = post(path).header("X-Api-Key", client).contentType(MediaType.APPLICATION_JSON).content(json);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mvc.perform(request);
    }

    ResultActions pay(String client, String key, String json) throws Exception {
        return call("/payments", client, key, json);
    }

    static String payment(String account, long cents) {
        return "{\"account\": \"" + account + "\", \"amountCents\": " + cents + "}";
    }

    static ResultActions isProblem(ResultActions actions, int status, String type) throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPES + type));
    }

    @Test
    void a_retry_with_the_same_key_replays_the_first_answer_without_charging_again() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        MockHttpServletResponse first = pay("shop-1", key, payment("acct-1", 2500))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        assertThat(first.getHeader("Idempotent-Replayed"), anyOf(nullValue(), is("false")));
        MockHttpServletResponse again = pay("shop-1", key, payment("acct-1", 2500))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andReturn().getResponse();
        assertEquals(first.getHeader("Location"), again.getHeader("Location"));
        assertEquals((String) JsonPath.read(first.getContentAsString(), "$.id"),
                JsonPath.read(again.getContentAsString(), "$.id"));
        assertEquals((String) JsonPath.read(first.getContentAsString(), "$.chargeId"),
                JsonPath.read(again.getContentAsString(), "$.chargeId"));
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void the_same_request_written_differently_is_still_the_same_request() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        pay("shop-1", key, "{\"account\": \"acct-2\", \"amountCents\": 700}").andExpect(status().isCreated());
        pay("shop-1", key, "{ \"amountCents\":700,\n  \"account\":\"acct-2\" }")
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"));
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void the_same_key_with_a_different_body_is_a_422() throws Exception {
        String key = newKey();
        pay("shop-1", key, payment("acct-3", 1000)).andExpect(status().isCreated());
        int before = gateway.charges();
        isProblem(pay("shop-1", key, payment("acct-3", 9000)), 422, "idempotency-key-reused");
        assertEquals(before, gateway.charges());
    }

    @Test
    void the_same_key_on_another_operation_is_a_422_too() throws Exception {
        String key = newKey();
        pay("shop-1", key, payment("acct-4", 1000)).andExpect(status().isCreated());
        isProblem(call("/refunds", "shop-1", key, "{\"paymentId\": \"pay_1\", \"amountCents\": 1000}"), 422,
                "idempotency-key-reused");
    }

    @Test
    void keys_belong_to_one_client() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        String a = pay("shop-1", key, payment("acct-5", 1200)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String b = pay("shop-2", key, payment("acct-5", 1200)).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();
        assertNotEquals((String) JsonPath.read(a, "$.id"), JsonPath.read(b, "$.id"));
        assertEquals(before + 2, gateway.charges());
    }

    @Test
    void a_missing_or_blank_key_is_a_400_and_charges_nothing() throws Exception {
        int before = gateway.charges();
        isProblem(pay("shop-1", null, payment("acct-6", 100)), 400, "idempotency-key-missing");
        isProblem(pay("shop-1", "  ", payment("acct-6", 100)), 400, "idempotency-key-missing");
        assertEquals(before, gateway.charges());
    }

    @Test
    void a_decline_is_an_answer_too_and_is_replayed_without_a_second_charge() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        isProblem(pay("shop-1", key, payment("acct-7", 1013)), 402, "card-declined");
        isProblem(pay("shop-1", key, payment("acct-7", 1013)), 402, "card-declined")
                .andExpect(header().string("Idempotent-Replayed", "true"));
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void after_a_503_the_same_key_may_be_retried_and_charges_once() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        gateway.failNextCharge();
        isProblem(pay("shop-1", key, payment("acct-8", 4400)), 503, "gateway-unavailable")
                .andExpect(header().exists("Retry-After"));
        pay("shop-1", key, payment("acct-8", 4400))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        pay("shop-1", key, payment("acct-8", 4400))
                .andExpect(header().string("Idempotent-Replayed", "true"));
        assertEquals(before + 1, gateway.charges());
    }

    @Test
    void keys_are_remembered_for_24_hours() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        String first = pay("shop-3", key, payment("acct-9", 300)).andReturn().getResponse().getContentAsString();
        ticker.advance(Duration.ofHours(23));
        pay("shop-3", key, payment("acct-9", 300)).andExpect(header().string("Idempotent-Replayed", "true"));
        assertEquals(before + 1, gateway.charges());
        ticker.advance(Duration.ofHours(1));
        String later = pay("shop-3", key, payment("acct-9", 300))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andReturn().getResponse().getContentAsString();
        assertNotEquals((String) JsonPath.read(first, "$.id"), JsonPath.read(later, "$.id"));
        assertEquals(before + 2, gateway.charges());
    }

    @Test
    void refunds_are_idempotent_as_well() throws Exception {
        String key = newKey();
        int before = gateway.refunds();
        String json = "{\"paymentId\": \"pay_42\", \"amountCents\": 500}";
        call("/refunds", "shop-1", key, json).andExpect(status().isCreated());
        call("/refunds", "shop-1", key, json)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true"));
        assertEquals(before + 1, gateway.refunds());
    }

    @Test
    void ten_concurrent_requests_with_one_key_charge_exactly_once() throws Exception {
        String key = newKey();
        int before = gateway.charges();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MockHttpServletResponse>> answers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            answers.add(pool.submit(() -> {
                start.await();
                return pay("shop-9", key, payment("acct-10", 6100)).andReturn().getResponse();
            }));
        }
        start.countDown();
        int created = 0;
        for (Future<MockHttpServletResponse> answer : answers) {
            MockHttpServletResponse response = answer.get();
            assertThat(response.getStatus(), anyOf(is(201), is(409)));
            if (response.getStatus() == 409) {
                assertThat(response.getHeader("Retry-After"), not(nullValue()));
            } else {
                created++;
            }
        }
        pool.shutdown();
        assertThat(created > 0, is(true));
        assertEquals(before + 1, gateway.charges());
    }
}
