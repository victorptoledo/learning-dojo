package dojo;

import static dojo.GradingSupport.code;
import static dojo.GradingSupport.instanceFieldTypes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentGradingTest {
    private static final Payment PAYMENT = new Payment("acc-9", 2599, "EUR");

    /** The grader's own fake SDK: scripted status codes, recorded calls. */
    static final class FakeSdk extends LegacyPayClient {
        final Deque<Integer> statuses;
        final List<String> calls = new ArrayList<>();

        FakeSdk(Integer... statuses) {
            this.statuses = new ArrayDeque<>(List.of(statuses));
        }

        @Override
        public int submit(String account, String amount, String currencyCode) {
            calls.add(account + " " + amount + " " + currencyCode);
            super.submit(account, amount, currencyCode);
            return statuses.isEmpty() ? OK : statuses.pop();
        }
    }

    @Test
    void the_adapter_translates_amounts_and_status_codes() {
        var sdk = new FakeSdk(LegacyPayClient.OK, LegacyPayClient.DECLINED, LegacyPayClient.BUSY, LegacyPayClient.OK);
        var adapter = new LegacyPayAdapter(sdk);
        assertEquals(new Receipt("LP-1", 2599), adapter.charge(PAYMENT));
        assertThrows(PaymentDeclinedException.class, () -> adapter.charge(PAYMENT));
        assertThrows(GatewayUnavailableException.class, () -> adapter.charge(PAYMENT));
        assertEquals(new Receipt("LP-4", 7), adapter.charge(new Payment("acc-2", 7, "USD")));
        adapter.charge(new Payment("acc-3", 100_000, "GBP"));
        assertEquals(List.of("acc-9 25.99 EUR", "acc-9 25.99 EUR", "acc-9 25.99 EUR", "acc-2 0.07 USD",
                "acc-3 1000.00 GBP"), sdk.calls);
    }

    @Test
    void retrying_stops_at_the_first_success_or_the_last_attempt() {
        var sdk = new FakeSdk(LegacyPayClient.BUSY, LegacyPayClient.BUSY, LegacyPayClient.OK);
        assertEquals("LP-3", new RetryingGateway(new LegacyPayAdapter(sdk), 3).charge(PAYMENT).reference());

        var alwaysBusy = new FakeSdk(LegacyPayClient.BUSY, LegacyPayClient.BUSY, LegacyPayClient.BUSY, LegacyPayClient.BUSY);
        assertThrows(GatewayUnavailableException.class,
                () -> new RetryingGateway(new LegacyPayAdapter(alwaysBusy), 2).charge(PAYMENT));
        assertEquals(2, alwaysBusy.calls.size());
        assertThrows(IllegalArgumentException.class, () -> new RetryingGateway(payment -> null, 0));
    }

    @Test
    void retrying_never_repeats_a_declined_payment() {
        int[] calls = {0};
        PaymentGateway declining = payment -> {
            calls[0]++;
            throw new PaymentDeclinedException("no");
        };
        assertThrows(PaymentDeclinedException.class, () -> new RetryingGateway(declining, 5).charge(PAYMENT));
        assertEquals(1, calls[0]);
    }

    @Test
    void decorators_wrap_any_gateway_and_their_order_matters() {
        var outside = new MeteredGateway(new RetryingGateway(new LegacyPayAdapter(
                new FakeSdk(LegacyPayClient.BUSY, LegacyPayClient.BUSY, LegacyPayClient.OK)), 3));
        outside.charge(PAYMENT);
        assertEquals(1, outside.successes(), "metering outside the retries counts payments");
        assertEquals(0, outside.failures());

        var inside = new MeteredGateway(new LegacyPayAdapter(
                new FakeSdk(LegacyPayClient.BUSY, LegacyPayClient.BUSY, LegacyPayClient.OK)));
        new RetryingGateway(inside, 3).charge(PAYMENT);
        assertEquals(1, inside.successes(), "metering inside the retries counts attempts");
        assertEquals(2, inside.failures());
    }

    @Test
    void the_metered_gateway_rethrows_what_it_counts() {
        var boom = new PaymentDeclinedException("declined");
        var metered = new MeteredGateway(payment -> {
            throw boom;
        });
        assertSame(boom, assertThrows(PaymentDeclinedException.class, () -> metered.charge(PAYMENT)));
        assertEquals(1, metered.failures());
    }

    @Test
    void checkout_validates_and_delegates() {
        List<Payment> seen = new ArrayList<>();
        var checkout = new CheckoutService(payment -> {
            seen.add(payment);
            return new Receipt("R-1", payment.cents());
        });
        assertEquals(new Receipt("R-1", 2599), checkout.pay(PAYMENT));
        assertThrows(IllegalArgumentException.class, () -> checkout.pay(new Payment("acc", 0, "EUR")));
        assertEquals(List.of(PAYMENT), seen);
    }

    @Test
    void only_the_adapter_knows_the_sdk() {
        assertTrue(PaymentGateway.class.isInterface(), "PaymentGateway must be an interface");
        for (Class<?> type : List.of(LegacyPayAdapter.class, RetryingGateway.class, MeteredGateway.class)) {
            assertTrue(PaymentGateway.class.isAssignableFrom(type), type.getSimpleName() + " must implement PaymentGateway");
        }
        assertEquals(List.of(PaymentGateway.class), instanceFieldTypes(CheckoutService.class),
                "CheckoutService should hold only a PaymentGateway");
        for (String name : List.of("dojo.CheckoutService", "dojo.RetryingGateway", "dojo.MeteredGateway")) {
            assertFalse(code(name).contains("LegacyPayClient"), name + " must not know the SDK: that is the adapter's job");
        }
        assertTrue(instanceFieldTypes(RetryingGateway.class).contains(PaymentGateway.class),
                "RetryingGateway should wrap a PaymentGateway");
        assertTrue(instanceFieldTypes(MeteredGateway.class).contains(PaymentGateway.class),
                "MeteredGateway should wrap a PaymentGateway");
    }
}
