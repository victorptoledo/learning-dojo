package dojo;

import java.time.Clock;
import org.springframework.stereotype.Component;

/** Asks the rates service through retries and the breaker, and falls back on the last known rate. */
@Component
public class ResilientRates {
    private final RateProvider provider;
    private final RetryPolicy retry;
    private final CircuitBreaker breaker;
    private final Sleeper sleeper;
    private final LastRateRepository lastRates;
    private final Clock clock;

    public ResilientRates(RateProvider provider, RetryPolicy retry, CircuitBreaker breaker, Sleeper sleeper,
                          LastRateRepository lastRates, Clock clock) {
        this.provider = provider;
        this.retry = retry;
        this.breaker = breaker;
        this.sleeper = sleeper;
        this.lastRates = lastRates;
        this.clock = clock;
    }

    public RateQuote rate(String from, String to) {
        throw new UnsupportedOperationException("todo rate " + from + " to " + to);
    }
}
