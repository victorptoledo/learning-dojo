package dojo;

import java.time.Clock;
import java.time.Duration;

/** One token bucket per client (an API key, a user id, an IP address). */
public final class RateLimiter {

    public RateLimiter(long capacity, long refillTokens, Duration refillPeriod, Clock clock) {
        throw new UnsupportedOperationException("todo limiter");
    }

    public Decision check(String client) {
        throw new UnsupportedOperationException("todo check " + client);
    }
}
