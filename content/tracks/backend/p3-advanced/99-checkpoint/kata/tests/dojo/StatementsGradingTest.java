package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Each test starts with an empty database, a closed breaker and an empty SLO window. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestBeans.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StatementsGradingTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    @Autowired
    EntryRepository entries;

    @Autowired
    LastRateRepository lastRates;

    @Autowired
    FakeRates rates;

    @Autowired
    TestClock clock;

    @Autowired
    TestBeans.RecordingSleeper sleeper;

    long ada;
    long bob;

    @BeforeEach
    void seed() {
        ada = accounts.save(new Account("ada", "Main", "USD", 12345, 50000)).getId();
        bob = accounts.save(new Account("bob", "Savings", "USD", 777, 0)).getId();
        entries.save(new Entry(ada, LocalDate.parse("2026-11-01"), "Salary", 300000));
        entries.save(new Entry(ada, LocalDate.parse("2026-11-02"), "Refund O'Brien Ltd", 2500));
        entries.save(new Entry(ada, LocalDate.parse("2026-11-03"), "Coffee", -450));
        entries.save(new Entry(ada, LocalDate.parse("2026-11-04"), "Saved 100% of it", -1000));
        entries.save(new Entry(ada, LocalDate.parse("2026-11-05"), "Rent", -120000));
        entries.save(new Entry(bob, LocalDate.parse("2026-11-02"), "Bob secret bonus", 999999));
    }

    ResultActions as(String user, String url) throws Exception {
        return mvc.perform(get(url).header("X-User", user));
    }

    List<String> descriptions(String user, String url) throws Exception {
        String json = as(user, url).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].description");
    }

    double number(String json, String path) {
        return ((Number) JsonPath.read(json, path)).doubleValue();
    }

    // ---- API security -------------------------------------------------------------------------------

    @Test
    void the_owner_sees_the_account_but_not_the_internal_fields() throws Exception {
        as("ada", "/accounts/" + ada)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ada))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balanceCents").value(12345))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.overdraftLimitCents").doesNotExist());
    }

    @Test
    void another_users_account_answers_exactly_like_a_missing_one_on_every_endpoint() throws Exception {
        String missing = as("ada", "/accounts/999999").andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        for (String path : List.of("", "/entries", "/balance?in=EUR")) {
            String foreign = as("ada", "/accounts/" + bob + path).andExpect(status().isNotFound())
                    .andReturn().getResponse().getContentAsString();
            assertEquals((String) JsonPath.read(missing, "$.detail"), JsonPath.read(foreign, "$.detail"), path);
        }
        mvc.perform(patch("/accounts/" + bob).header("X-User", "ada").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\": \"mine now\"}")).andExpect(status().isNotFound());
        assertEquals("Savings", accounts.findById(bob).orElseThrow().getNickname());
        assertEquals(0, rates.calls(), "no rate is fetched for an account the caller cannot see");
    }

    @Test
    void a_request_without_a_user_is_401() throws Exception {
        mvc.perform(get("/accounts/" + ada)).andExpect(status().isUnauthorized());
    }

    @Test
    void a_patch_changes_the_nickname_and_nothing_else() throws Exception {
        mvc.perform(patch("/accounts/" + ada).header("X-User", "ada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"Holiday\", \"owner\": \"mallory\", \"balanceCents\": 99999999,"
                                + " \"overdraftLimitCents\": 1000000, \"currency\": \"EUR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Holiday"))
                .andExpect(jsonPath("$.balanceCents").value(12345));
        Account saved = accounts.findById(ada).orElseThrow();
        assertEquals("Holiday", saved.getNickname());
        assertEquals("ada", saved.getOwner());
        assertEquals(12345, saved.getBalanceCents());
        assertEquals(50000, saved.getOverdraftLimitCents());
        assertEquals("USD", saved.getCurrency());
    }

    @Test
    void a_blank_or_too_long_nickname_is_400() throws Exception {
        for (String nickname : List.of("\"\"", "\"" + "x".repeat(41) + "\"")) {
            mvc.perform(patch("/accounts/" + ada).header("X-User", "ada").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nickname\": " + nickname + "}")).andExpect(status().isBadRequest());
        }
        assertEquals("Main", accounts.findById(ada).orElseThrow().getNickname());
    }

    @Test
    void entries_sort_and_size_follow_the_contract() throws Exception {
        String base = "/accounts/" + ada + "/entries";
        assertEquals(List.of("Rent", "Saved 100% of it", "Coffee", "Refund O'Brien Ltd", "Salary"), descriptions("ada", base));
        assertEquals(List.of("Rent", "Saved 100% of it"), descriptions("ada", base + "?size=2"));
        assertEquals(List.of("Rent", "Saved 100% of it", "Coffee", "Refund O'Brien Ltd", "Salary"),
                descriptions("ada", base + "?sort=amount"));
        assertEquals(List.of("Salary", "Refund O'Brien Ltd"), descriptions("ada", base + "?sort=-amount&size=2"));
        assertEquals(List.of("Salary", "Refund O'Brien Ltd"), descriptions("ada", base + "?sort=bookedOn&size=2"));
        as("ada", base + "?size=0").andExpect(status().isBadRequest());
        as("ada", base + "?size=101").andExpect(status().isBadRequest());
        assertEquals(5, descriptions("ada", base + "?size=100").size());
    }

    @Test
    void a_sort_outside_the_allow_list_is_400() throws Exception {
        for (String sort : List.of("description", "id",
                "case when (select count(*) from accounts where owner = 'bob') > 0 then amount else id end")) {
            mvc.perform(get("/accounts/" + ada + "/entries").header("X-User", "ada").param("sort", sort))
                    .andExpect(status().isBadRequest());
        }
    }

    List<String> search(String q) throws Exception {
        String json = mvc.perform(get("/accounts/" + ada + "/entries").header("X-User", "ada").param("q", q))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].description");
    }

    @Test
    void the_search_text_is_bound_as_a_parameter() throws Exception {
        assertEquals(List.of("Refund O'Brien Ltd"), search("o'brien"));
        assertEquals(List.of(), search("' or account_id <> 0 or description like '"));
        assertEquals(List.of("Saved 100% of it"), search("100%"), "% is a character, not a wildcard");
        assertEquals(List.of(), search("_"), "_ is a character, not a wildcard");
        assertEquals(List.of("Coffee"), search("COFF"));
    }

    // ---- Resilience ---------------------------------------------------------------------------------

    @Test
    void the_same_currency_needs_no_rate() throws Exception {
        as("ada", "/accounts/" + ada + "/balance?in=USD").andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents").value(12345)).andExpect(jsonPath("$.stale").value(false));
        assertEquals(0, rates.calls());
    }

    @Test
    void timeouts_are_retried_with_backoff_and_the_rate_is_kept_for_every_instance() throws Exception {
        rates.failNext(2);
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amountCents").value(11280))
                .andExpect(jsonPath("$.stale").value(false));
        assertEquals(3, rates.calls());
        assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), sleeper.waits());
        var last = lastRates.findById("USD:EUR").orElseThrow(() -> new AssertionError("the rate is not in the database"));
        assertEquals(0, new BigDecimal("0.9137").compareTo(last.getRate()));
    }

    @Test
    void an_unknown_currency_is_400_and_not_retried() throws Exception {
        as("ada", "/accounts/" + ada + "/balance?in=XYZ").andExpect(status().isBadRequest());
        assertEquals(1, rates.calls());
        assertTrue(sleeper.waits().isEmpty());
    }

    @Test
    void without_a_known_rate_an_outage_is_503_with_retry_after() throws Exception {
        rates.down(true);
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"));
        assertEquals(3, rates.calls());
    }

    @Test
    void during_an_outage_the_last_known_rate_is_served_as_stale() throws Exception {
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(jsonPath("$.stale").value(false));
        rates.setRate("USD", "EUR", new BigDecimal("0.5"));
        rates.down(true);
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents").value(11280))
                .andExpect(jsonPath("$.stale").value(true));
    }

    @Test
    void the_breaker_opens_stops_calls_and_closes_after_a_good_trial() throws Exception {
        String url = "/accounts/" + ada + "/balance?in=EUR";
        rates.down(true);
        as("ada", url).andExpect(status().isServiceUnavailable());
        as("ada", url).andExpect(status().isServiceUnavailable());
        assertEquals(5, rates.calls(), "the fifth failure opens the circuit");
        int waits = sleeper.waits().size();
        as("ada", url).andExpect(status().isServiceUnavailable());
        assertEquals(5, rates.calls(), "an open circuit fails fast");
        assertEquals(waits, sleeper.waits().size(), "an open circuit is not retried");
        rates.down(false);
        clock.advance(Duration.ofSeconds(30));
        as("ada", url).andExpect(status().isOk()).andExpect(jsonPath("$.stale").value(false));
        assertEquals(6, rates.calls());
    }

    // ---- SRE ------------------------------------------------------------------------------------------

    @Test
    void the_slo_counts_server_failures_as_bad_and_pages_on_a_fast_burn() throws Exception {
        mvc.perform(get("/slo")).andExpect(status().isOk());
        as("ada", "/accounts/" + ada).andExpect(status().isOk());
        as("ada", "/accounts/" + ada + "/entries").andExpect(status().isOk());
        as("ada", "/accounts/" + bob).andExpect(status().isNotFound());
        as("ada", "/accounts/" + ada + "/entries?sort=id").andExpect(status().isBadRequest());
        mvc.perform(get("/accounts/" + ada)).andExpect(status().isUnauthorized());
        rates.down(true);
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(status().isServiceUnavailable());

        String json = mvc.perform(get("/slo")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(6, number(json, "$.total"), "only /accounts requests count, and /slo is not one");
        assertEquals(5, number(json, "$.good"), "4xx answers are the client's fault: they do not burn the budget");
        assertEquals(5.0 / 6, number(json, "$.availability"), 1e-6);
        assertEquals((1.0 / 6) / 0.01, number(json, "$.burnRate"), 1e-6);
        assertEquals(1 - (1.0 / 6) / 0.01, number(json, "$.budgetRemaining"), 1e-6);
        assertTrue((Boolean) JsonPath.read(json, "$.paging"));
    }

    @Test
    void the_slo_window_slides_and_a_slow_burn_does_not_page() throws Exception {
        for (int i = 0; i < 9; i++) {
            as("ada", "/accounts/" + ada).andExpect(status().isOk());
        }
        rates.down(true);
        as("ada", "/accounts/" + ada + "/balance?in=EUR").andExpect(status().isServiceUnavailable());

        String json = mvc.perform(get("/slo")).andReturn().getResponse().getContentAsString();
        assertEquals(10, number(json, "$.total"));
        assertEquals(10.0, number(json, "$.burnRate"), 1e-6);
        assertFalse((Boolean) JsonPath.read(json, "$.paging"), "a burn rate of 10 is below the page threshold");

        clock.advance(Duration.ofMinutes(61));
        json = mvc.perform(get("/slo")).andReturn().getResponse().getContentAsString();
        assertEquals(0, number(json, "$.total"));
        assertEquals(1.0, number(json, "$.availability"), 1e-9);
        assertEquals(0.0, number(json, "$.burnRate"), 1e-9);
        assertEquals(1.0, number(json, "$.budgetRemaining"), 1e-9);
    }
}
