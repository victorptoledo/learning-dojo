package dojo;

import java.time.Duration;

/** At most {@code maxAttempts} attempts; the waits grow by {@code multiplier}, capped at {@code maxDelay}. */
public record RetryPolicy(int maxAttempts, Duration initialDelay, double multiplier, Duration maxDelay) {

    /** The wait before attempt number {@code attempt} (2 is the first retry). */
    public Duration delayBefore(int attempt) {
        if (attempt < 2) {
            return Duration.ZERO;
        }
        double millis = initialDelay.toMillis() * Math.pow(multiplier, attempt - 2);
        return Duration.ofMillis((long) Math.min(millis, maxDelay.toMillis()));
    }
}
