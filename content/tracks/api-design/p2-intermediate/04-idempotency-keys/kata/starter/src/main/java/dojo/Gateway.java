package dojo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * The card processor. Written for you. Every successful or declined charge moves money (or
 * at least costs a fee), so the tests count them. Amounts ending in 13 are declined. It is
 * slow, and tests can make the next call fail as if the network dropped.
 */
@Component
public class Gateway {
    private final AtomicInteger charges = new AtomicInteger();
    private final AtomicInteger refunds = new AtomicInteger();
    private final AtomicBoolean failNext = new AtomicBoolean();

    public static final class CardDeclinedException extends RuntimeException {
        public CardDeclinedException(String message) {
            super(message);
        }
    }

    public static final class GatewayUnavailableException extends RuntimeException {
        public GatewayUnavailableException() {
            super("the card processor did not answer");
        }
    }

    /** Charges the account and returns the charge id. */
    public String charge(String account, long amountCents) {
        pause();
        if (failNext.getAndSet(false)) {
            throw new GatewayUnavailableException();
        }
        int number = charges.incrementAndGet();
        if (amountCents % 100 == 13) {
            throw new CardDeclinedException("card of " + account + " declined");
        }
        return "ch_" + number;
    }

    /** Refunds part or all of a charge and returns the refund id. */
    public String refund(String paymentId, long amountCents) {
        pause();
        return "re_" + refunds.incrementAndGet();
    }

    public int charges() {
        return charges.get();
    }

    public int refunds() {
        return refunds.get();
    }

    /** Makes the next charge fail with GatewayUnavailableException without charging. */
    public void failNextCharge() {
        failNext.set(true);
    }

    private static void pause() {
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
