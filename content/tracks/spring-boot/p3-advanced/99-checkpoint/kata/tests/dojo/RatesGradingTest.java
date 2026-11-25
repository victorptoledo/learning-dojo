package dojo;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RatesGradingTest.Clocks.class)
class RatesGradingTest {
    /** A clock the tests move by hand. */
    static final class HandClock extends Clock {
        private volatile Instant now;

        HandClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @TestConfiguration
    static class Clocks {
        @Bean
        @Primary
        HandClock handClock() {
            return new HandClock(Instant.parse("2026-03-02T09:00:00Z"));
        }
    }

    static final RequestPostProcessor TRADER = httpBasic("trader", "trader-pass");
    static final RequestPostProcessor VIEWER = httpBasic("viewer", "viewer-pass");

    @Autowired
    MockMvc mvc;

    @Autowired
    HandClock clock;

    @Autowired
    RateService service;

    @Autowired
    RateRepository repository;

    @Autowired
    AuditTrail audit;

    @Autowired
    MeterRegistry meters;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PlatformTransactionManager transactions;

    void set(String code, String perEuro) throws Exception {
        mvc.perform(put("/rates/" + code).with(TRADER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perEuro\": " + perEuro + "}"))
                .andExpect(status().isOk());
    }

    long statements() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics().getPrepareStatementCount();
    }

    double updatesCounted(String code) {
        Counter counter = meters.find("desk.rate.updates").tag("currency", code).counter();
        return counter == null ? 0 : counter.count();
    }

    void freshenEveryRate() throws Exception {
        for (Rate rate : repository.findAll()) {
            set(rate.getCode(), rate.getPerEuro().toPlainString());
        }
    }

    @Test
    void anyone_reads_a_rate() throws Exception {
        set("USD", "1.08");
        mvc.perform(get("/rates/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("USD"))
                .andExpect(jsonPath("$.perEuro").value(1.08));
        mvc.perform(get("/rates/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("no rate for XXX"));
    }

    @Test
    void only_traders_change_a_rate() throws Exception {
        set("SEK", "11.2");
        String body = "{\"perEuro\": 99}";
        mvc.perform(put("/rates/SEK").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/rates/SEK").with(httpBasic("trader", "wrong")).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/rates/SEK").with(VIEWER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/rates/SEK")).andExpect(jsonPath("$.perEuro").value(11.2));
    }

    @Test
    void a_bad_rate_is_400() throws Exception {
        mvc.perform(put("/rates/NZD").with(TRADER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perEuro\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "viewer", roles = "USER")
    void the_trader_rule_guards_the_service_method_itself() {
        assertThrows(AccessDeniedException.class, () -> service.update("SVC", BigDecimal.TEN),
                "update must be protected on the method (@PreAuthorize), not only by a URL rule");
        assertTrue(repository.findById("SVC").isEmpty());
    }

    @Test
    void reads_come_from_the_cache_until_the_rate_changes() throws Exception {
        set("CHF", "0.93");
        mvc.perform(get("/rates/CHF")).andExpect(jsonPath("$.perEuro").value(0.93));
        long before = statements();
        mvc.perform(get("/rates/CHF")).andExpect(status().isOk()).andExpect(jsonPath("$.perEuro").value(0.93));
        assertEquals(before, statements(), "a second read of the same rate must not run any SQL (@Cacheable)");

        set("CHF", "0.94");
        mvc.perform(get("/rates/CHF")).andExpect(jsonPath("$.perEuro").value(0.94));
    }

    @Test
    void a_missing_rate_is_not_cached() throws Exception {
        mvc.perform(get("/rates/NOK")).andExpect(status().isNotFound());
        set("NOK", "11.7");
        mvc.perform(get("/rates/NOK")).andExpect(status().isOk()).andExpect(jsonPath("$.perEuro").value(11.7));
    }

    @Test
    void committed_changes_are_audited_and_counted() throws Exception {
        set("GBP", "0.85");
        set("GBP", "0.86");
        assertTrue(audit.entries().contains("GBP set to 0.85 by trader"), audit.entries().toString());
        assertTrue(audit.entries().contains("GBP 0.85 -> 0.86 by trader"), audit.entries().toString());
        assertEquals(2.0, updatesCounted("GBP"), "counter desk.rate.updates with tag currency=GBP");
    }

    @Test
    @WithMockUser(username = "trader", roles = {"USER", "TRADER"})
    void a_rolled_back_change_leaves_no_trace() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            service.update("RBK", new BigDecimal("3.5"));
            tx.setRollbackOnly();
        });
        assertTrue(repository.findById("RBK").isEmpty());
        assertTrue(audit.entries().stream().noneMatch(entry -> entry.startsWith("RBK")),
                "a change that rolled back must not reach the audit trail: " + audit.entries());
        assertEquals(0.0, updatesCounted("RBK"), "a change that rolled back must not be counted");
    }

    @Test
    void a_stale_rate_takes_the_service_down() throws Exception {
        set("HLT", "4.2");
        freshenEveryRate();
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.rates.status").value("UP"));

        clock.advance(Duration.ofMinutes(59));
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.components.rates.status").value("UP"));

        clock.advance(Duration.ofMinutes(2));
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.components.rates.status").value("DOWN"))
                .andExpect(jsonPath("$.components.rates.details.stale", hasItem("HLT")));

        freshenEveryRate();
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.components.rates.status").value("UP"));
    }
}
