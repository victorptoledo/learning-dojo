package dojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** The payment service for the kata. It lives outside the process, so a crash does not reset it. */
@Component
public class FakePayments implements Payments {
    private final Map<String, Long> charges = new HashMap<>();
    private final Set<String> refunded = new HashSet<>();
    private boolean declineNext;
    private boolean crashAfterNextCharge;
    private int chargeCalls;

    @Override
    public synchronized void charge(String sagaId, long cents) {
        chargeCalls++;
        if (!charges.containsKey(sagaId)) {
            if (declineNext) {
                declineNext = false;
                throw new StepFailedException("card declined");
            }
            charges.put(sagaId, cents);
        }
        if (crashAfterNextCharge) {
            crashAfterNextCharge = false;
            throw new CrashException("the process died after the card was charged");
        }
    }

    @Override
    public synchronized void refund(String sagaId) {
        if (charges.containsKey(sagaId)) {
            refunded.add(sagaId);
        }
    }

    /** The money kept: every charge that was not refunded. */
    public synchronized long capturedCents() {
        return charges.entrySet().stream().filter(c -> !refunded.contains(c.getKey()))
                .mapToLong(Map.Entry::getValue).sum();
    }

    public synchronized boolean refunded(String sagaId) {
        return refunded.contains(sagaId);
    }

    public synchronized int chargeCalls() {
        return chargeCalls;
    }

    public synchronized void declineNext() {
        declineNext = true;
    }

    public synchronized void crashAfterNextCharge() {
        crashAfterNextCharge = true;
    }
}
