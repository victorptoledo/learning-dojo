package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RateLimiterGradingTest {
    @Test
    void a_bucket_starts_full_and_refuses_when_empty() {
        var clock = new TestClock();
        var bucket = new TokenBucket(3, 1, Duration.ofSeconds(1), clock);
        assertEquals(3, bucket.available());
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
        assertEquals(0, bucket.available());
    }

    @Test
    void it_refills_continuously_and_never_above_capacity() {
        var clock = new TestClock();
        var bucket = new TokenBucket(10, 5, Duration.ofSeconds(1), clock);
        assertTrue(bucket.tryAcquire(10));
        clock.advance(Duration.ofMillis(500));
        assertEquals(2, bucket.available(), "5 per second for half a second is 2.5 tokens");
        clock.advance(Duration.ofMillis(100));
        assertEquals(3, bucket.available());
        clock.advance(Duration.ofHours(1));
        assertEquals(10, bucket.available());
    }

    @Test
    void retry_after_says_how_long_until_the_next_token() {
        var clock = new TestClock();
        var bucket = new TokenBucket(5, 5, Duration.ofSeconds(1), clock);
        assertEquals(Duration.ZERO, bucket.retryAfter());
        bucket.tryAcquire(5);
        assertEquals(Duration.ofMillis(200), bucket.retryAfter());
        clock.advance(Duration.ofMillis(150));
        assertEquals(Duration.ofMillis(50), bucket.retryAfter());
        clock.advance(Duration.ofMillis(50));
        assertEquals(Duration.ZERO, bucket.retryAfter());
    }

    @Test
    void acquiring_several_tokens_is_all_or_nothing() {
        var clock = new TestClock();
        var bucket = new TokenBucket(5, 1, Duration.ofSeconds(1), clock);
        assertTrue(bucket.tryAcquire(3));
        assertFalse(bucket.tryAcquire(3), "only 2 left");
        assertEquals(2, bucket.available(), "a refused request takes nothing");
    }

    @Test
    void impossible_buckets_are_rejected() {
        var clock = new TestClock();
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, 0, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, 1, Duration.ZERO, clock));
    }

    @Test
    void the_limiter_keeps_one_bucket_per_client() {
        var clock = new TestClock();
        var limiter = new RateLimiter(2, 1, Duration.ofSeconds(10), clock);
        assertEquals(new Decision(true, 1, Duration.ZERO), limiter.check("ada"));
        assertEquals(new Decision(true, 0, Duration.ZERO), limiter.check("ada"));
        assertEquals(new Decision(false, 0, Duration.ofSeconds(10)), limiter.check("ada"));
        assertTrue(limiter.check("linus").allowed(), "linus has his own bucket");
        clock.advance(Duration.ofSeconds(4));
        assertEquals(new Decision(false, 0, Duration.ofSeconds(6)), limiter.check("ada"));
        clock.advance(Duration.ofSeconds(6));
        assertTrue(limiter.check("ada").allowed());
    }

    @Test
    void concurrent_requests_never_get_more_than_the_capacity() throws Exception {
        var clock = new TestClock();
        var limiter = new RateLimiter(100, 1, Duration.ofHours(1), clock);
        var allowed = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 1000; i++) {
                executor.submit(() -> {
                    if (limiter.check("hot-key").allowed()) {
                        allowed.incrementAndGet();
                    }
                });
            }
        }
        assertEquals(100, allowed.get());
    }
}
