package dojo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** The rates service for the kata: fixed rates, and it can be told to time out or go down. */
@Component
public class FakeRates implements RateProvider {
    private final Map<String, BigDecimal> rates = new HashMap<>(Map.of(
            "USD:EUR", new BigDecimal("0.9137"),
            "USD:GBP", new BigDecimal("0.7854"),
            "EUR:USD", new BigDecimal("1.0945")));
    private int failuresLeft;
    private boolean down;
    private int calls;

    @Override
    public synchronized BigDecimal rate(String from, String to) {
        calls++;
        if (down) {
            throw new TransientException("rates service unavailable");
        }
        if (failuresLeft > 0) {
            failuresLeft--;
            throw new TransientException("rates service timed out");
        }
        BigDecimal rate = rates.get(from + ":" + to);
        if (rate == null) {
            throw new UnknownCurrencyException(from, to);
        }
        return rate;
    }

    /** The next {@code times} calls time out. */
    public synchronized void failNext(int times) {
        failuresLeft = times;
    }

    /** Every call fails until {@code down(false)}. */
    public synchronized void down(boolean down) {
        this.down = down;
    }

    public synchronized void setRate(String from, String to, BigDecimal rate) {
        rates.put(from + ":" + to, rate);
    }

    /** How many times the service was called. */
    public synchronized int calls() {
        return calls;
    }
}
