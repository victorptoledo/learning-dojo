package dojo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dojo.billing.Billing;
import dojo.billing.NewInvoice;
import dojo.notifications.Reminders;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {"billing.invoice-prefix=ACME", "billing.payment-term-days=14"})
@AutoConfigureMockMvc
@Import(ProductionGradingTest.FixedClock.class)
class ProductionGradingTest {
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-03-02T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    static final RequestPostProcessor CLERK = httpBasic("clerk", "clerk-pass");
    static final RequestPostProcessor ADMIN = httpBasic("admin", "admin-pass");

    @Autowired
    MockMvc mvc;

    @Autowired
    Billing billing;

    @Autowired
    Reminders reminders;

    @Autowired
    PlatformTransactionManager transactions;

    String issue(String customer, long amountCents) throws Exception {
        String json = mvc.perform(post("/invoices").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\": \"" + customer + "\", \"amountCents\": " + amountCents + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.number");
    }

    void maintenance(boolean on) throws Exception {
        mvc.perform(post("/admin/maintenance").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": " + on + "}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void invoices_follow_the_configured_prefix_and_payment_term() throws Exception {
        String number = issue("Globex", 125_000);
        assertTrue(number.matches("ACME-\\d{4}"), "number from billing.invoice-prefix: " + number);
        mvc.perform(get("/invoices/" + number).with(CLERK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuedOn").value("2026-03-02"))
                .andExpect(jsonPath("$.dueOn").value("2026-03-16"));
        mvc.perform(get("/invoices/" + number + "/text").with(CLERK))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("| Invoice " + number + " | Globex | 1250.00 EUR")))
                .andExpect(content().string(Matchers.containsString("Thank you for your business.")));
    }

    @Test
    void an_issued_invoice_gets_a_reminder() throws Exception {
        String number = issue("Initech", 9_900);
        mvc.perform(get("/reminders").with(CLERK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.invoice == '" + number + "')].remindOn").value("2026-03-13"));
    }

    @Test
    void an_invoice_that_rolls_back_gets_no_reminder() {
        String[] number = new String[1];
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            number[0] = billing.issue(new NewInvoice("Hooli", 100)).number();
            tx.setRollbackOnly();
        });
        assertTrue(reminders.planned().stream().noneMatch(reminder -> reminder.invoice().equals(number[0])),
                "notifications must hear about an invoice only once it is committed: " + reminders.planned());
    }

    @Test
    void anyone_sees_the_health_status_but_only_admins_see_the_details() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").with(CLERK)).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").with(ADMIN)).andExpect(jsonPath("$.components.db.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    void the_probes_are_public() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void only_admins_read_metrics_and_nothing_else_is_exposed() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").with(CLERK)).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").with(ADMIN)).andExpect(status().isOk());
        for (String endpoint : new String[] {"env", "beans", "configprops", "heapdump", "loggers", "mappings"}) {
            mvc.perform(get("/actuator/" + endpoint).with(ADMIN)).andExpect(status().isNotFound());
        }
    }

    @Test
    void maintenance_takes_the_service_out_of_rotation_without_killing_it() throws Exception {
        mvc.perform(post("/admin/maintenance").with(CLERK).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"on\": true}"))
                .andExpect(status().isForbidden());
        maintenance(true);
        try {
            mvc.perform(get("/actuator/health/readiness"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        } finally {
            maintenance(false);
        }
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }
}
