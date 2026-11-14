package dojo;

import java.util.function.Supplier;

/** Calls through the circuit breaker and retries transient failures with backoff. */
public final class ResilientClient {
    private final RetryPolicy retry;
    private final CircuitBreaker breaker;
    private final Sleeper sleeper;

    public ResilientClient(RetryPolicy retry, CircuitBreaker breaker, Sleeper sleeper) {
        this.retry = retry;
        this.breaker = breaker;
        this.sleeper = sleeper;
    }

    public <T> T execute(Supplier<T> call) {
        TransientException last = null;
        for (int attempt = 1; attempt <= retry.maxAttempts(); attempt++) {
            if (attempt > 1) {
                sleeper.sleep(retry.delayBefore(attempt));
            }
            try {
                return breaker.call(call);
            } catch (TransientException e) {
                last = e;
            }
        }
        throw last;
    }
}
