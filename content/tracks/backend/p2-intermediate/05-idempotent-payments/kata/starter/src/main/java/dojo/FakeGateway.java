package dojo;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** A gateway for the kata: counts charges, declines amounts that end in 13, and is slow. */
@Component
public class FakeGateway implements PaymentGateway {
    private final AtomicInteger charges = new AtomicInteger();

    @Override
    public String charge(String account, long amountCents) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (amountCents % 100 == 13) {
            charges.incrementAndGet();
            throw new DeclinedException("card declined for " + account);
        }
        return "ch_" + charges.incrementAndGet();
    }

    /** How many times charge was called. */
    public int charges() {
        return charges.get();
    }
}
