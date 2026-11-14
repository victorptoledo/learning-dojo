package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ResilientClientTest {
    @Test
    void a_working_call_is_returned() {
        var client = new ResilientClient(new RetryPolicy(3, Duration.ofMillis(10), 2, Duration.ofSeconds(1)),
                new CircuitBreaker(5, Duration.ofSeconds(10), new TestClock()), duration -> {});
        assertEquals("pong", client.execute(() -> "pong"));
    }
}
