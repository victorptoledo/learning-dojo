package dojo;

import java.util.function.Supplier;

/** Calls through the circuit breaker and retries transient failures with backoff. */
public final class ResilientClient {

    public ResilientClient(RetryPolicy retry, CircuitBreaker breaker, Sleeper sleeper) {
        throw new UnsupportedOperationException("todo client");
    }

    public <T> T execute(Supplier<T> call) {
        throw new UnsupportedOperationException("todo execute");
    }
}
