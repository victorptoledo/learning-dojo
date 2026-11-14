package dojo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TokenBucketTest {
    @Test
    void two_tokens_then_a_refusal() {
        var bucket = new TokenBucket(2, 1, Duration.ofMinutes(1), new TestClock());
        assertTrue(bucket.tryAcquire());
        assertTrue(bucket.tryAcquire());
        assertFalse(bucket.tryAcquire());
    }
}
