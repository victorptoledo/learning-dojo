package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Stops calling a dependency that keeps failing. CLOSED: calls go through and consecutive failures
 * are counted. OPEN: calls fail fast with CircuitOpenException. After {@code openDuration}, HALF_OPEN:
 * one trial call decides between CLOSED and OPEN.
 */
public final class CircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private State state = State.CLOSED;
    private int failures;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public synchronized <T> T call(Supplier<T> action) {
        if (state() == State.OPEN) {
            throw new CircuitOpenException();
        }
        try {
            T result = action.get();
            state = State.CLOSED;
            failures = 0;
            return result;
        } catch (RuntimeException e) {
            failures++;
            if (state == State.HALF_OPEN || failures >= failureThreshold) {
                state = State.OPEN;
                openedAt = clock.instant();
            }
            throw e;
        }
    }

    public synchronized State state() {
        if (state == State.OPEN && !clock.instant().isBefore(openedAt.plus(openDuration))) {
            state = State.HALF_OPEN;
        }
        return state;
    }
}
