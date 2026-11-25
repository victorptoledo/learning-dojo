package dojo;

import java.math.BigDecimal;
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
        if (from.equals(to)) {
            return new RateQuote(BigDecimal.ONE, false);
        }
        String pair = from + ":" + to;
        try {
            BigDecimal rate = fetch(from, to);
            lastRates.save(new LastRate(pair, rate, clock.instant()));
            return new RateQuote(rate, false);
        } catch (TransientException | CircuitOpenException unavailable) {
            return lastRates.findById(pair)
                    .map(last -> new RateQuote(last.getRate(), true))
                    .orElseThrow(() -> StatementsException.ratesUnavailable(breaker.openDuration()));
        }
    }

    /** Retries timeouts with backoff; an open circuit or an unknown currency ends it at once. */
    private BigDecimal fetch(String from, String to) {
        TransientException last = null;
        for (int attempt = 1; attempt <= retry.maxAttempts(); attempt++) {
            if (attempt > 1) {
                sleeper.sleep(retry.delayBefore(attempt));
            }
            try {
                return breaker.call(() -> provider.rate(from, to));
            } catch (TransientException e) {
                last = e;
            }
        }
        throw last;
    }
}
