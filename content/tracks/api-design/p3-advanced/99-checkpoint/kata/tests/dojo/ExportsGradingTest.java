package dojo;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dojo.Partners.Credentials;
import dojo.WebhookSender.Delivery;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class ExportsGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";
    static final String ORDERS_CSV = "{\"dataset\": \"orders\", \"format\": \"csv\"}";

    @Autowired MockMvc mvc;
    @Autowired Partners partners;
    @Autowired Exporter exporter;
    @Autowired BackgroundJobs jobs;
    @Autowired WebhookSender sender;
    @Autowired Ticker ticker;

    record Partner(String id, Credentials credentials) {
        String key() {
            return credentials.apiKey();
        }
    }

    Partner partner(String... scopes) {
        String id = "partner-" + UUID.randomUUID();
        return new Partner(id, partners.register(id, scopes));
    }

    Partner readWrite() {
        return partner("exports:read", "exports:write");
    }

    ResultActions as(Partner partner, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + partner.key()));
    }

    ResultActions start(Partner partner, String json) throws Exception {
        return as(partner, post("/exports").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String startedId(Partner partner, String json) throws Exception {
        String body = start(partner, json).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    static ResultActions isProblem(ResultActions actions, int status, String type) throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPES + type))
                .andExpect(jsonPath("$.status").value(status));
    }

    // ---- authentication -------------------------------------------------------------------------

    @Test
    void a_key_in_the_query_string_is_refused_even_a_valid_one() throws Exception {
        Partner partner = readWrite();
        isProblem(mvc.perform(post("/exports").param("apiKey", partner.key())
                .contentType(MediaType.APPLICATION_JSON).content(ORDERS_CSV)), 400, "key-in-query");
        isProblem(as(partner, get("/exports/exp_1").param("apiKey", partner.key())), 400, "key-in-query");
    }

    @Test
    void a_missing_malformed_or_unknown_key_is_401_with_a_bearer_challenge() throws Exception {
        for (var request : List.of(
                get("/exports/exp_1"),
                get("/exports/exp_1").header("Authorization", "Basic YWRtaW46YWRtaW4="),
                get("/exports/exp_1").header("Authorization", "Bearer dk_test_not_a_key"),
                post("/exports").header("Authorization", "Bearer dk_test_not_a_key")
                        .contentType(MediaType.APPLICATION_JSON).content(ORDERS_CSV))) {
            isProblem(mvc.perform(request), 401, "unauthorized")
                    .andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
                    .andExpect(header().string("WWW-Authenticate", containsString("error=\"invalid_token\"")))
                    .andExpect(header().doesNotExist("RateLimit"));
        }
    }

    @Test
    void a_key_without_the_scope_is_403_naming_the_scope() throws Exception {
        isProblem(start(partner("exports:read"), ORDERS_CSV), 403, "insufficient-scope")
                .andExpect(header().string("WWW-Authenticate", containsString("error=\"insufficient_scope\"")))
                .andExpect(header().string("WWW-Authenticate", containsString("scope=\"exports:write\"")));
        isProblem(as(partner("exports:write"), get("/exports/exp_1")), 403, "insufficient-scope")
                .andExpect(header().string("WWW-Authenticate", containsString("scope=\"exports:read\"")));
    }

    // ---- long-running operation -----------------------------------------------------------------

    @Test
    void an_export_is_accepted_at_once_and_runs_in_the_background() throws Exception {
        Partner partner = readWrite();
        int runsBefore = exporter.runs();
        String body = start(partner, ORDERS_CSV)
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", matchesPattern(".*/exports/exp_[A-Za-z0-9]+")))
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.dataset").value("orders"))
                .andExpect(jsonPath("$.format").value("csv"))
                .andExpect(jsonPath("$.resultUrl").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        assertEquals(runsBefore, exporter.runs(), "the export must not run inside the request");

        as(partner, get("/exports/" + id))
                .andExpect(status().isOk())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        jobs.runDue();
        assertEquals(runsBefore + 1, exporter.runs(), "the background job runs the export once");
        as(partner, get("/exports/" + id))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Retry-After"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.resultUrl").value("https://files.dojo.example/exports/" + id + ".csv"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void a_failed_export_says_why_as_a_problem() throws Exception {
        Partner partner = readWrite();
        String id = startedId(partner, "{\"dataset\": \"invoices\", \"format\": \"ndjson\"}");
        jobs.runDue();
        as(partner, get("/exports/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.resultUrl").doesNotExist())
                .andExpect(jsonPath("$.error.type").value(TYPES + "export-failed"))
                .andExpect(jsonPath("$.error.title").value("Export failed"))
                .andExpect(jsonPath("$.error.detail").value("the invoices warehouse is offline"));
    }

    @Test
    void an_invalid_request_is_a_400_problem_and_starts_nothing() throws Exception {
        Partner partner = readWrite();
        start(partner, "{\"dataset\": \"orders\", \"format\": \"xlsx\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        jobs.runDue();
        assertTrue(sender.deliveriesTo(partner.id()).isEmpty(), "nothing was exported, so nothing is announced");
    }

    @Test
    void another_partners_export_is_as_unknown_as_a_missing_one() throws Exception {
        Partner owner = readWrite();
        Partner other = readWrite();
        String id = startedId(owner, ORDERS_CSV);
        isProblem(as(other, get("/exports/" + id)), 404, "export-not-found")
                .andExpect(jsonPath("$.title").value("Export not found"))
                .andExpect(jsonPath("$.dataset").doesNotExist());
        isProblem(as(other, get("/exports/exp_999999")), 404, "export-not-found")
                .andExpect(jsonPath("$.title").value("Export not found"));
        as(owner, get("/exports/" + id)).andExpect(status().isOk());
    }

    // ---- rate limiting --------------------------------------------------------------------------

    @Test
    void every_answer_tells_the_client_its_budget_and_a_burst_ends_in_429() throws Exception {
        Partner partner = partner("exports:read");
        as(partner, get("/exports/exp_0"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("RateLimit-Policy", "\"default\";q=10;w=10"))
                .andExpect(header().string("RateLimit", "\"default\";r=9;t=1"));
        for (int i = 2; i < 10; i++) {
            as(partner, get("/exports/exp_0")).andExpect(status().isNotFound());
        }
        as(partner, get("/exports/exp_0"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("RateLimit", "\"default\";r=0;t=10"));

        isProblem(as(partner, get("/exports/exp_0")), 429, "rate-limited")
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(header().string("RateLimit-Policy", "\"default\";q=10;w=10"))
                .andExpect(header().string("RateLimit", "\"default\";r=0;t=10"));

        ticker.advance(Duration.ofSeconds(1));
        as(partner, get("/exports/exp_0"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("RateLimit", "\"default\";r=0;t=10"));
        isProblem(as(partner, get("/exports/exp_0")), 429, "rate-limited");

        ticker.advance(Duration.ofMillis(3500));
        as(partner, get("/exports/exp_0"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("RateLimit", "\"default\";r=2;t=8"));
    }

    @Test
    void the_retry_after_of_a_429_is_the_time_until_the_next_token() throws Exception {
        Partner partner = partner("exports:read");
        for (int i = 0; i < 10; i++) {
            as(partner, get("/exports/exp_0"));
        }
        ticker.advance(Duration.ofMillis(200));
        isProblem(as(partner, get("/exports/exp_0")), 429, "rate-limited")
                .andExpect(header().string("Retry-After", "1"));
        ticker.advance(Duration.ofMillis(10_000));
        as(partner, get("/exports/exp_0")).andExpect(header().string("RateLimit", "\"default\";r=9;t=1"));
    }

    @Test
    void each_partner_has_its_own_bucket_and_it_is_checked_before_the_scope() throws Exception {
        Partner busy = partner("exports:read");
        for (int i = 0; i < 10; i++) {
            as(busy, get("/exports/exp_0"));
        }
        isProblem(as(busy, get("/exports/exp_0")), 429, "rate-limited");
        isProblem(start(busy, ORDERS_CSV), 429, "rate-limited");

        Partner fresh = partner("exports:read");
        isProblem(start(fresh, ORDERS_CSV), 403, "insufficient-scope")
                .andExpect(header().string("RateLimit", "\"default\";r=9;t=1"));
        as(fresh, get("/exports/exp_0")).andExpect(header().string("RateLimit", "\"default\";r=8;t=2"));
    }

    // ---- webhooks -------------------------------------------------------------------------------

    static Map<String, String> headers(Delivery delivery) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(delivery.headers());
        return headers;
    }

    static String expectedSignature(String secret, String id, String timestamp, String body) throws Exception {
        byte[] key = Base64.getDecoder().decode(secret.substring("whsec_".length()));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] signature = mac.doFinal((id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(signature);
    }

    static void assertSigned(Partner partner, Delivery delivery) throws Exception {
        Map<String, String> headers = headers(delivery);
        String id = headers.get("webhook-id");
        String timestamp = headers.get("webhook-timestamp");
        assertTrue(id != null && !id.isBlank(), "missing webhook-id");
        assertTrue(timestamp != null && timestamp.matches("\\d+"), "webhook-timestamp must be seconds since the epoch");
        assertEquals(expectedSignature(partner.credentials().webhookSecret(), id, timestamp, delivery.body()),
                headers.get("webhook-signature"),
                "webhook-signature must be v1, plus the base64 HMAC-SHA256 of id.timestamp.body");
    }

    @Test
    void a_finished_export_is_announced_by_a_signed_webhook() throws Exception {
        Partner partner = readWrite();
        String id = startedId(partner, ORDERS_CSV);
        assertTrue(sender.deliveriesTo(partner.id()).isEmpty(), "nothing is sent before the export finishes");
        jobs.runDue();

        List<Delivery> deliveries = sender.deliveriesTo(partner.id());
        assertEquals(1, deliveries.size(), "one delivery per finished export");
        Delivery delivery = deliveries.getFirst();
        Map<String, String> headers = headers(delivery);
        assertEquals(String.valueOf(ticker.now().getEpochSecond()), headers.get("webhook-timestamp"));
        assertTrue(headers.getOrDefault("content-type", "").startsWith("application/json"), "content-type");
        assertSigned(partner, delivery);

        String body = delivery.body();
        assertEquals(headers.get("webhook-id"), JsonPath.read(body, "$.id"), "webhook-id is the event id");
        assertEquals("export.succeeded", JsonPath.read(body, "$.type"));
        assertEquals(ticker.now(), Instant.parse(JsonPath.read(body, "$.createdAt")));
        assertEquals(id, JsonPath.read(body, "$.data.exportId"));
        assertEquals("SUCCEEDED", JsonPath.read(body, "$.data.status"));
        assertEquals("https://files.dojo.example/exports/" + id + ".csv", JsonPath.read(body, "$.data.resultUrl"));
    }

    @Test
    void a_failed_export_is_announced_too() throws Exception {
        Partner partner = readWrite();
        String id = startedId(partner, "{\"dataset\": \"invoices\", \"format\": \"csv\"}");
        jobs.runDue();
        Delivery delivery = sender.deliveriesTo(partner.id()).getFirst();
        assertSigned(partner, delivery);
        assertEquals("export.failed", JsonPath.read(delivery.body(), "$.type"));
        assertEquals(id, JsonPath.read(delivery.body(), "$.data.exportId"));
        assertEquals("FAILED", JsonPath.read(delivery.body(), "$.data.status"));
    }

    @Test
    void every_event_has_its_own_id() throws Exception {
        Partner partner = readWrite();
        startedId(partner, ORDERS_CSV);
        startedId(partner, "{\"dataset\": \"customers\", \"format\": \"ndjson\"}");
        jobs.runDue();
        List<Delivery> deliveries = sender.deliveriesTo(partner.id());
        assertEquals(2, deliveries.size());
        assertNotEquals(headers(deliveries.get(0)).get("webhook-id"), headers(deliveries.get(1)).get("webhook-id"));
    }

    @Test
    void a_failed_delivery_is_retried_after_10_then_20_seconds_with_the_same_id() throws Exception {
        Partner partner = readWrite();
        sender.failNext(partner.id(), 2);
        startedId(partner, ORDERS_CSV);
        long start = ticker.now().getEpochSecond();
        jobs.runDue();
        assertEquals(1, sender.deliveriesTo(partner.id()).size());

        ticker.advance(Duration.ofSeconds(9));
        jobs.runDue();
        assertEquals(1, sender.deliveriesTo(partner.id()).size(), "the first retry waits 10 seconds");
        ticker.advance(Duration.ofSeconds(1));
        jobs.runDue();
        assertEquals(2, sender.deliveriesTo(partner.id()).size(), "first retry after 10 seconds");

        ticker.advance(Duration.ofSeconds(19));
        jobs.runDue();
        assertEquals(2, sender.deliveriesTo(partner.id()).size(), "the second retry waits 20 seconds more");
        ticker.advance(Duration.ofSeconds(1));
        jobs.runDue();
        List<Delivery> deliveries = sender.deliveriesTo(partner.id());
        assertEquals(3, deliveries.size(), "second retry after 20 more seconds");
        assertEquals(204, deliveries.get(2).status());

        ticker.advance(Duration.ofHours(1));
        jobs.runDue();
        assertEquals(3, sender.deliveriesTo(partner.id()).size(), "a delivery that succeeded is not sent again");

        String id = headers(deliveries.get(0)).get("webhook-id");
        long[] offsets = {0, 10, 30};
        for (int i = 0; i < 3; i++) {
            Delivery delivery = deliveries.get(i);
            assertEquals(id, headers(delivery).get("webhook-id"), "a retry keeps the event id");
            assertEquals(deliveries.get(0).body(), delivery.body(), "a retry sends the same event");
            assertEquals(String.valueOf(start + offsets[i]), headers(delivery).get("webhook-timestamp"),
                    "each attempt carries its own timestamp");
            assertSigned(partner, delivery);
        }
    }

    @Test
    void delivery_gives_up_after_five_attempts() throws Exception {
        Partner partner = readWrite();
        sender.failNext(partner.id(), 1000);
        startedId(partner, ORDERS_CSV);
        jobs.runDue();
        for (int delay : new int[] {10, 20, 40, 80}) {
            ticker.advance(Duration.ofSeconds(delay));
            jobs.runDue();
        }
        assertEquals(5, sender.deliveriesTo(partner.id()).size(), "retries after 10, 20, 40 and 80 seconds");
        ticker.advance(Duration.ofHours(6));
        jobs.runDue();
        assertEquals(5, sender.deliveriesTo(partner.id()).size(), "five attempts, then it stops");
    }
}
