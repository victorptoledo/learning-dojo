package dojo;

import java.time.Clock;
import java.time.Duration;

/**
 * A token bucket: holds up to {@code capacity} tokens, starts full, and refills continuously at
 * {@code refillTokens} per {@code refillPeriod}. Each request takes tokens; no tokens, no request.
 * Safe to use from many threads.
 */
public final class TokenBucket {

    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod, Clock clock) {
        throw new UnsupportedOperationException("todo bucket of " + capacity);
    }

    /** Takes {@code tokens} if they are all there; otherwise takes nothing. */
    public boolean tryAcquire(long tokens) {
        throw new UnsupportedOperationException("todo acquire " + tokens);
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /** Whole tokens available now. */
    public long available() {
        throw new UnsupportedOperationException("todo available");
    }

    /** How long until one token is available: ZERO if one already is. */
    public Duration retryAfter() {
        throw new UnsupportedOperationException("todo retry after");
    }
}
