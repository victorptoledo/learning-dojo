package dojo;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** A token bucket per partner: 10 tokens, refilled continuously at one per second. */
@Component
public class RateLimiter {
    static final int CAPACITY = 10;
    static final String POLICY = "\"default\";q=" + CAPACITY + ";w=" + CAPACITY;

    /** The outcome of one request: allowed or not, and what the headers say. */
    record Decision(boolean allowed, long remaining, long secondsUntilFull, long retryAfterSeconds) {
        String header() {
            return "\"default\";r=" + remaining + ";t=" + secondsUntilFull;
        }
    }

    /** Tokens are counted in thousandths, so that time in milliseconds refills them exactly. */
    private record Bucket(long milliTokens, Instant at) {}

    private final Ticker ticker;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(Ticker ticker) {
        this.ticker = ticker;
    }

    Decision take(String partnerId) {
        Instant now = ticker.now();
        Decision[] decision = new Decision[1];
        buckets.compute(partnerId, (id, bucket) -> {
            long tokens = refilled(bucket, now);
            boolean allowed = tokens >= 1000;
            long left = allowed ? tokens - 1000 : tokens;
            decision[0] = new Decision(allowed, left / 1000,
                    ceilSeconds(CAPACITY * 1000L - left), Math.max(1, ceilSeconds(1000 - left)));
            return new Bucket(left, now);
        });
        return decision[0];
    }

    private static long refilled(Bucket bucket, Instant now) {
        if (bucket == null) {
            return CAPACITY * 1000L;
        }
        long millis = now.toEpochMilli() - bucket.at().toEpochMilli();
        return Math.min(CAPACITY * 1000L, bucket.milliTokens() + millis);
    }

    private static long ceilSeconds(long milliTokens) {
        return (milliTokens + 999) / 1000;
    }
}
