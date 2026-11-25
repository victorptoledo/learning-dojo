package dojo;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A wrong setting must stop the application at startup, before it takes any traffic. */
class StartupGradingTest {
    static void assertStartupFails(String setting, String mentioned) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DojoApplication.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run("--" + setting, "--spring.jmx.enabled=false", "--logging.level.root=off")) {
            fail("the application started with " + setting);
        } catch (RuntimeException e) {
            StringBuilder messages = new StringBuilder();
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                messages.append(cause.getMessage()).append('\n');
            }
            assertTrue(messages.toString().contains(mentioned),
                    "the startup error should name the setting (" + mentioned + "):\n" + messages);
        }
    }

    @Test
    void a_payment_term_of_zero_days_stops_the_application() {
        assertStartupFails("billing.payment-term-days=0", "paymentTermDays");
    }

    @Test
    void a_payment_term_over_ninety_days_stops_the_application() {
        assertStartupFails("billing.payment-term-days=120", "paymentTermDays");
    }

    @Test
    void a_blank_invoice_prefix_stops_the_application() {
        assertStartupFails("billing.invoice-prefix=", "invoicePrefix");
    }

    @Test
    void a_lowercase_invoice_prefix_stops_the_application() {
        assertStartupFails("billing.invoice-prefix=acme", "invoicePrefix");
    }
}
