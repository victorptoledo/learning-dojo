package dojo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * A token bucket: holds up to {@code capacity} tokens, starts full, and refills continuously at
 * {@code refillTokens} per {@code refillPeriod}. Each request takes tokens; no tokens, no request.
 * Safe to use from many threads.
 */
public final class TokenBucket {
    // Tokens are counted in "nanoseconds of refill" so all the arithmetic stays exact:
    // one token = periodNanos units, and every elapsed nanosecond adds refillTokens units.
    private final long periodNanos;
    private final long refillTokens;
    private final long capacityUnits;
    private final Clock clock;
    private long units;
    private Instant last;

    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod, Clock clock) {
        if (capacity < 1 || refillTokens < 1 || refillPeriod.isNegative() || refillPeriod.isZero()) {
            throw new IllegalArgumentException("capacity, refill and period must be positive");
        }
        this.periodNanos = refillPeriod.toNanos();
        this.refillTokens = refillTokens;
        this.capacityUnits = Math.multiplyExact(capacity, periodNanos);
        this.clock = clock;
        this.units = capacityUnits;
        this.last = clock.instant();
    }

    /** Takes {@code tokens} if they are all there; otherwise takes nothing. */
    public synchronized boolean tryAcquire(long tokens) {
        refill();
        long needed = Math.multiplyExact(tokens, periodNanos);
        if (units < needed) {
            return false;
        }
        units -= needed;
        return true;
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /** Whole tokens available now. */
    public synchronized long available() {
        refill();
        return units / periodNanos;
    }

    /** How long until one token is available: ZERO if one already is. */
    public synchronized Duration retryAfter() {
        refill();
        long missing = periodNanos - units;
        if (missing <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofNanos((missing + refillTokens - 1) / refillTokens);
    }

    private void refill() {
        Instant now = clock.instant();
        long elapsed = Duration.between(last, now).toNanos();
        if (elapsed > 0) {
            long added = elapsed > capacityUnits / refillTokens ? capacityUnits : elapsed * refillTokens;
            units = Math.min(capacityUnits, units + added);
            last = now;
        }
    }
}
