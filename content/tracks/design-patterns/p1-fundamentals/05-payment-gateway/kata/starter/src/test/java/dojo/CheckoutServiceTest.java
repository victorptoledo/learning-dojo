package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * These tests describe the design you are refactoring towards, so they do not compile until
 * PaymentGateway, LegacyPayAdapter and the decorators exist (see README.md).
 */
class CheckoutServiceTest {
    /** An SDK that answers with the given status codes, one per call, and records the amounts. */
    static final class ScriptedClient extends LegacyPayClient {
        final Deque<Integer> statuses;
        final List<String> amounts = new ArrayList<>();

        ScriptedClient(Integer... statuses) {
            this.statuses = new ArrayDeque<>(List.of(statuses));
        }

        @Override
        public int submit(String account, String amount, String currencyCode) {
            amounts.add(amount);
            super.submit(account, amount, currencyCode);
            return statuses.isEmpty() ? OK : statuses.pop();
        }
    }

    @Test
    void the_adapter_speaks_the_sdk_language() {
        var client = new ScriptedClient();
        PaymentGateway gateway = new LegacyPayAdapter(client);
        assertEquals(new Receipt("LP-1", 1205), gateway.charge(new Payment("acc-1", 1205, "EUR")));
        assertEquals(List.of("12.05"), client.amounts);
    }

    @Test
    void busy_providers_are_retried_and_counted() {
        var client = new ScriptedClient(LegacyPayClient.BUSY, LegacyPayClient.OK);
        var metered = new MeteredGateway(new RetryingGateway(new LegacyPayAdapter(client), 3));
        var checkout = new CheckoutService(metered);

        assertEquals("LP-2", checkout.pay(new Payment("acc-1", 500, "EUR")).reference());
        assertEquals(1, metered.successes());
        assertEquals(0, metered.failures());
    }

    @Test
    void declined_payments_are_not_retried() {
        var client = new ScriptedClient(LegacyPayClient.DECLINED, LegacyPayClient.OK);
        var checkout = new CheckoutService(new RetryingGateway(new LegacyPayAdapter(client), 3));
        assertThrows(PaymentDeclinedException.class, () -> checkout.pay(new Payment("acc-1", 500, "EUR")));
        assertEquals(1, client.amounts.size());
    }
}
