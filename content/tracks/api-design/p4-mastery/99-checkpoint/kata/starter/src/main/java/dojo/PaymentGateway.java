package dojo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * The card processor. Written for you. Every charge moves money, so the tests count them.
 * The tests can make the next call fail: {@link #failNext()} as if the provider were down,
 * {@link #breakNext()} as if our own payments database had a bug.
 */
@Component
public class PaymentGateway {
    public static final class GatewayUnavailableException extends RuntimeException {
        public GatewayUnavailableException() {
            super("the card processor did not answer within 10 s");
        }
    }

    private final AtomicInteger charges = new AtomicInteger();
    private final AtomicBoolean failNext = new AtomicBoolean();
    private final AtomicBoolean breakNext = new AtomicBoolean();

    /** Charges the amount and returns the charge id. */
    public String charge(String tenant, long invoiceId, long amountCents) {
        if (failNext.getAndSet(false)) {
            throw new GatewayUnavailableException();
        }
        if (breakNext.getAndSet(false)) {
            throw new IllegalStateException("connection pool exhausted: jdbc:postgresql://10.0.3.7:5432/payments"
                    + " user=billing_rw (invoice " + invoiceId + ")");
        }
        return "ch_" + charges.incrementAndGet();
    }

    public int charges() {
        return charges.get();
    }

    /** The next charge fails with GatewayUnavailableException and charges nothing. */
    public void failNext() {
        failNext.set(true);
    }

    /** The next charge fails with an unexpected IllegalStateException and charges nothing. */
    public void breakNext() {
        breakNext.set(true);
    }
}
