package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Stops calling a dependency that keeps failing. CLOSED: calls go through and consecutive failures
 * are counted. OPEN: calls fail fast with CircuitOpenException. After {@code openDuration}, HALF_OPEN:
 * one trial call decides between CLOSED and OPEN.
 */
public final class CircuitBreaker {

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        throw new UnsupportedOperationException("todo breaker");
    }

    public <T> T call(Supplier<T> action) {
        throw new UnsupportedOperationException("todo call");
    }

    public State state() {
        throw new UnsupportedOperationException("todo state");
    }
}
