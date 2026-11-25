package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * The breaker from the Resilient Client kata, already written. Only a {@link TransientException}
 * counts as a failure: a wrong request (an unknown currency) says nothing about the dependency's health.
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

    /** Runs the action, or throws CircuitOpenException without running it while the circuit is open. */
    public synchronized <T> T call(Supplier<T> action) {
        if (state() == State.OPEN) {
            throw new CircuitOpenException();
        }
        try {
            T result = action.get();
            state = State.CLOSED;
            failures = 0;
            return result;
        } catch (TransientException e) {
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

    public Duration openDuration() {
        return openDuration;
    }
}
