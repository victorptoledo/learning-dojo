package dojo;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One token bucket per client (an API key, a user id, an IP address). */
public final class RateLimiter {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;
    private final Clock clock;

    public RateLimiter(long capacity, long refillTokens, Duration refillPeriod, Clock clock) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = refillPeriod;
        this.clock = clock;
    }

    public Decision check(String client) {
        TokenBucket bucket = buckets.computeIfAbsent(client,
                key -> new TokenBucket(capacity, refillTokens, refillPeriod, clock));
        synchronized (bucket) {
            boolean allowed = bucket.tryAcquire();
            Duration retryAfter = allowed ? Duration.ZERO : bucket.retryAfter();
            return new Decision(allowed, bucket.available(), retryAfter);
        }
    }
}
