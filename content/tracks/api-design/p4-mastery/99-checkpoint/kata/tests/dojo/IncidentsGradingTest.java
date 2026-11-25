package dojo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentsGradingTest {
    static final String TYPES = "https://api.dojo.example/problems/";

    @Autowired MockMvc mvc;
    @Autowired Tenants tenants;
    @Autowired InvoiceStore store;
    @Autowired PaymentGateway gateway;

    record Tenant(String id, String key) {}

    Tenant tenant() {
        String id = "tenant-" + UUID.randomUUID();
        return new Tenant(id, tenants.register(id));
    }

    Invoice invoice(Tenant tenant, long cents) {
        return store.add(tenant.id(), "Customer of " + tenant.id(), cents, "EUR", LocalDate.of(2026, 9, 30));
    }

    ResultActions as(Tenant tenant, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + tenant.key()));
    }

    ResultActions pay(Tenant tenant, long invoiceId, String key, long cents) throws Exception {
        var request = post("/invoices/" + invoiceId + "/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amountCents\": " + cents + "}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return as(tenant, request);
    }

    static ResultActions isProblem(ResultActions actions, int status, String type) throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(TYPES + type))
                .andExpect(jsonPath("$.status").value(status));
    }

    static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    // ---- INC-1: the accidental breaking change, and money and time ----------------------------

    @Test
    void inc1_an_invoice_is_back_in_the_v1_shape() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 12_345);
        String json = body(as(tenant, get("/invoices/" + invoice.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.id()))
                .andExpect(jsonPath("$.amountCents").value(12345))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.dueDate").value("2026-09-30"))
                .andExpect(jsonPath("$.amount").doesNotExist()));
        String issuedAt = JsonPath.read(json, "$.issuedAt");
        assertEquals(invoice.issuedAt(), Instant.parse(issuedAt), "issuedAt is the instant, in RFC 3339 UTC");
        assertTrue(issuedAt.endsWith("Z"), "issuedAt is written in UTC: " + issuedAt);
    }

    @Test
    void inc1_amounts_in_cents_survive_without_rounding() throws Exception {
        Tenant tenant = tenant();
        for (long cents : new long[] {1, 29, 1005, 999_999_999_99L}) {
            String json = body(as(tenant, post("/invoices").contentType(MediaType.APPLICATION_JSON).content(
                            "{\"customer\": \"Ada Ltd\", \"amountCents\": " + cents
                                    + ", \"currency\": \"EUR\", \"dueDate\": \"2026-12-31\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/invoices/")))
                    .andExpect(jsonPath("$.dueDate").value("2026-12-31")));
            assertEquals(cents, ((Number) JsonPath.read(json, "$.amountCents")).longValue());
            long id = ((Number) JsonPath.read(json, "$.id")).longValue();
            assertEquals(cents, store.find(id).orElseThrow().amountCents(), "stored exactly");
        }
        as(tenant, post("/invoices").contentType(MediaType.APPLICATION_JSON).content(
                "{\"customer\": \"Ada Ltd\", \"amount\": 12.30, \"currency\": \"EUR\", \"dueDate\": \"2026-12-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---- INC-2: the unbounded list --------------------------------------------------------------

    @Test
    void inc2_the_list_has_a_default_page_size_and_a_next_link() throws Exception {
        Tenant tenant = tenant();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            ids.add(invoice(tenant, 100 + i).id());
        }
        ResultActions first = as(tenant, get("/invoices")).andExpect(status().isOk());
        String json = body(first);
        List<Number> page = JsonPath.read(json, "$.data[*].id");
        assertEquals(50, page.size(), "the default page holds 50 invoices");
        assertEquals(ids.subList(0, 50), page.stream().map(Number::longValue).toList(), "by id, oldest first");
        String cursor = JsonPath.read(json, "$.nextCursor");
        assertNotNull(cursor, "a page with more after it has a nextCursor");
        assertFalse(cursor.matches("\\d+"), "the cursor is opaque, not a plain id or offset");
        String link = first.andReturn().getResponse().getHeader("Link");
        assertNotNull(link, "a Link header points at the next page");
        assertTrue(link.contains("rel=\"next\"") && link.contains(cursor), "Link: <...cursor...>; rel=\"next\"");

        String all = body(as(tenant, get("/invoices").param("limit", "200"))
                .andExpect(header().doesNotExist("Link")));
        assertEquals(120, ((List<?>) JsonPath.read(all, "$.data")).size());
        assertNull(JsonPath.read(all, "$.nextCursor"), "the last page has no next cursor");
    }

    @Test
    void inc2_the_page_size_is_bounded() throws Exception {
        Tenant tenant = tenant();
        for (String limit : List.of("0", "201", "100000", "-5")) {
            isProblem(as(tenant, get("/invoices").param("limit", limit)), 400, "invalid-parameter");
        }
        as(tenant, get("/invoices").param("limit", "1")).andExpect(status().isOk());
    }

    @Test
    void inc2_cursors_walk_every_invoice_once_while_new_ones_arrive() throws Exception {
        Tenant tenant = tenant();
        Set<Long> expected = new HashSet<>();
        for (int i = 0; i < 95; i++) {
            expected.add(invoice(tenant, 500 + i).id());
        }
        List<Long> seen = new ArrayList<>();
        String cursor = null;
        for (int pages = 0; pages < 20; pages++) {
            var request = get("/invoices").param("limit", "40");
            if (cursor != null) {
                request.param("cursor", cursor);
            }
            String json = body(as(tenant, request).andExpect(status().isOk()));
            List<Number> page = JsonPath.read(json, "$.data[*].id");
            page.forEach(id -> seen.add(id.longValue()));
            cursor = JsonPath.read(json, "$.nextCursor");
            if (cursor == null) {
                break;
            }
            for (int i = 0; i < 3; i++) {
                expected.add(invoice(tenant, 900 + i).id());
            }
        }
        assertEquals(new HashSet<>(seen).size(), seen.size(), "no invoice twice");
        assertEquals(expected, new HashSet<>(seen), "every invoice, including those added between pages");
        isProblem(as(tenant, get("/invoices").param("cursor", "not-a-cursor")), 400, "invalid-cursor");
    }

    // ---- INC-3: the leaky 500 -------------------------------------------------------------------

    @Test
    void inc3_an_unexpected_error_is_a_500_problem_that_reveals_nothing() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 700);
        gateway.breakNext();
        MockHttpServletResponse response = isProblem(pay(tenant, invoice.id(), UUID.randomUUID().toString(), 700),
                500, "internal-error").andReturn().getResponse();
        String json = response.getContentAsString();
        for (String secret : List.of("jdbc", "10.0.3.7", "billing_rw", "Exception", "at dojo.", "java.", "\"trace\"")) {
            assertFalse(json.contains(secret), "the 500 reveals \"" + secret + "\": " + json);
        }
    }

    // ---- INC-4: broken object-level authorisation -----------------------------------------------

    @Test
    void inc4_a_tenant_sees_and_pays_only_its_own_invoices() throws Exception {
        Tenant owner = tenant();
        Tenant other = tenant();
        Invoice invoice = invoice(owner, 4_200);
        String foreign = body(isProblem(as(other, get("/invoices/" + invoice.id())), 404, "invoice-not-found"));
        String missing = body(isProblem(as(other, get("/invoices/999999999")), 404, "invoice-not-found"));
        assertEquals(JsonPath.read(missing, "$.title"), (Object) JsonPath.read(foreign, "$.title"));
        assertFalse(foreign.contains(owner.id()), "the 404 says nothing about the owner");

        int charges = gateway.charges();
        isProblem(pay(other, invoice.id(), UUID.randomUUID().toString(), 4_200), 404, "invoice-not-found");
        assertEquals(charges, gateway.charges(), "nobody pays another tenant's invoice");
        as(owner, get("/invoices/" + invoice.id())).andExpect(jsonPath("$.status").value("OPEN"));

        String list = body(as(other, get("/invoices").param("limit", "200")));
        assertEquals(0, ((List<?>) JsonPath.read(list, "$.data")).size(), "a new tenant lists nothing");
    }

    // ---- INC-5: duplicate charges ---------------------------------------------------------------

    @Test
    void inc5_a_payment_needs_an_idempotency_key() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 1_000);
        int charges = gateway.charges();
        isProblem(pay(tenant, invoice.id(), null, 1_000), 400, "idempotency-key-missing");
        isProblem(pay(tenant, invoice.id(), " ", 1_000), 400, "idempotency-key-missing");
        assertEquals(charges, gateway.charges());
    }

    @Test
    void inc5_a_retry_with_the_same_key_replays_the_payment_without_charging() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 2_500);
        String key = UUID.randomUUID().toString();
        int charges = gateway.charges();
        String first = body(pay(tenant, invoice.id(), key, 2_500)
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.invoiceId").value(invoice.id()))
                .andExpect(jsonPath("$.amountCents").value(2500))
                .andExpect(jsonPath("$.currency").value("EUR")));
        String again = body(pay(tenant, invoice.id(), key, 2_500)
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replayed", "true")));
        assertEquals(JsonPath.read(first, "$.id"), (Object) JsonPath.read(again, "$.id"), "the same payment");
        assertEquals(JsonPath.read(first, "$.chargeId"), (Object) JsonPath.read(again, "$.chargeId"));
        assertEquals(charges + 1, gateway.charges(), "one charge for two requests");
        as(tenant, get("/invoices/" + invoice.id())).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void inc5_a_paid_invoice_is_not_charged_again_and_keys_are_not_reused() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 3_000);
        Invoice another = invoice(tenant, 3_000);
        String key = UUID.randomUUID().toString();
        pay(tenant, invoice.id(), key, 3_000).andExpect(status().isCreated());
        int charges = gateway.charges();

        isProblem(pay(tenant, invoice.id(), UUID.randomUUID().toString(), 3_000), 409, "invoice-already-paid");
        isProblem(pay(tenant, another.id(), key, 3_000), 422, "idempotency-key-reused");
        isProblem(pay(tenant, invoice.id(), key, 2_999), 422, "idempotency-key-reused");
        isProblem(pay(tenant, another.id(), UUID.randomUUID().toString(), 2_999), 422, "amount-mismatch");
        assertEquals(charges, gateway.charges());

        Tenant stranger = tenant();
        Invoice theirs = invoice(stranger, 3_000);
        pay(stranger, theirs.id(), key, 3_000).andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        assertEquals(charges + 1, gateway.charges(), "keys belong to one tenant");
    }

    // ---- INC-6: the retry storm -----------------------------------------------------------------

    @Test
    void inc6_a_provider_outage_is_a_503_with_retry_after_and_the_key_stays_usable() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 1_234);
        String key = UUID.randomUUID().toString();
        int charges = gateway.charges();
        gateway.failNext();
        isProblem(pay(tenant, invoice.id(), key, 1_234), 503, "payment-provider-unavailable")
                .andExpect(header().string("Retry-After", "30"));
        as(tenant, get("/invoices/" + invoice.id())).andExpect(jsonPath("$.status").value("OPEN"));

        pay(tenant, invoice.id(), key, 1_234)
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotent-Replayed"));
        assertEquals(charges + 1, gateway.charges(), "the retry after the outage charges once");
    }

    @Test
    void inc6_after_a_500_the_key_stays_usable_too() throws Exception {
        Tenant tenant = tenant();
        Invoice invoice = invoice(tenant, 800);
        String key = UUID.randomUUID().toString();
        gateway.breakNext();
        pay(tenant, invoice.id(), key, 800).andExpect(status().isInternalServerError());
        pay(tenant, invoice.id(), key, 800).andExpect(status().isCreated());
    }

    // ---- nothing else broke ---------------------------------------------------------------------

    @Test
    void authentication_still_works() throws Exception {
        isProblem(mvc.perform(get("/invoices")), 401, "unauthorized")
                .andExpect(header().string("WWW-Authenticate", allOf(containsString("Bearer"), not(containsString("Basic")))));
    }
}
