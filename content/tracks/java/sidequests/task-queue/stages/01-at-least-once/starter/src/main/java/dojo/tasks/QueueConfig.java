package dojo.tasks;

import java.time.Duration;

/**
 * How the queue behaves: how long a lease lasts, how many deliveries a task gets, and the
 * backoff between failed deliveries (used from stage 2).
 */
public record QueueConfig(Duration lease, int maxAttempts, Duration baseDelay, Duration maxDelay) {
}
