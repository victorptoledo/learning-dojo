package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ResilienceGradingTest {
    private static final RetryPolicy RETRY = new RetryPolicy(5, Duration.ofMillis(100), 2.0, Duration.ofMillis(500));

    private static Supplier<String> failing(AtomicInteger calls, int failures, RuntimeException error) {
        return () -> {
            if (calls.incrementAndGet() <= failures) {
                throw error;
            }
            return "ok";
        };
    }

    @Test
    void delays_grow_exponentially_and_are_capped() {
        assertEquals(Duration.ofMillis(100), RETRY.delayBefore(2));
        assertEquals(Duration.ofMillis(200), RETRY.delayBefore(3));
        assertEquals(Duration.ofMillis(400), RETRY.delayBefore(4));
        assertEquals(Duration.ofMillis(500), RETRY.delayBefore(5));
        assertEquals(Duration.ofMillis(500), RETRY.delayBefore(9));
    }

    @Test
    void transient_failures_are_retried_with_the_backoff_delays() {
        List<Duration> sleeps = new ArrayList<>();
        var client = new ResilientClient(RETRY, new CircuitBreaker(10, Duration.ofSeconds(30), new TestClock()), sleeps::add);
        var calls = new AtomicInteger();
        assertEquals("ok", client.execute(failing(calls, 2, new TransientException("timeout"))));
        assertEquals(3, calls.get());
        assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), sleeps);
    }

    @Test
    void permanent_failures_are_not_retried() {
        List<Duration> sleeps = new ArrayList<>();
        var client = new ResilientClient(RETRY, new CircuitBreaker(10, Duration.ofSeconds(30), new TestClock()), sleeps::add);
        var calls = new AtomicInteger();
        var bug = new IllegalArgumentException("400: bad request");
        assertSame(bug, assertThrows(IllegalArgumentException.class, () -> client.execute(failing(calls, 9, bug))));
        assertEquals(1, calls.get());
        assertEquals(List.of(), sleeps);
    }

    @Test
    void after_the_last_attempt_the_last_failure_is_thrown() {
        List<Duration> sleeps = new ArrayList<>();
        var client = new ResilientClient(RETRY, new CircuitBreaker(10, Duration.ofSeconds(30), new TestClock()), sleeps::add);
        var calls = new AtomicInteger();
        assertThrows(TransientException.class, () -> client.execute(failing(calls, 99, new TransientException("down"))));
        assertEquals(5, calls.get());
        assertEquals(4, sleeps.size());
    }

    @Test
    void the_breaker_opens_after_consecutive_failures_and_fails_fast() {
        var clock = new TestClock();
        var breaker = new CircuitBreaker(3, Duration.ofSeconds(30), clock);
        var calls = new AtomicInteger();
        Supplier<String> broken = failing(calls, 99, new TransientException("down"));
        for (int i = 0; i < 3; i++) {
            assertThrows(TransientException.class, () -> breaker.call(broken));
        }
        assertEquals(State.OPEN, breaker.state());
        assertThrows(CircuitOpenException.class, () -> breaker.call(broken));
        assertEquals(3, calls.get(), "an open circuit does not call the dependency");
    }

    @Test
    void a_success_resets_the_failure_count() {
        var breaker = new CircuitBreaker(3, Duration.ofSeconds(30), new TestClock());
        Supplier<String> down = () -> { throw new TransientException("down"); };
        assertThrows(TransientException.class, () -> breaker.call(down));
        assertThrows(TransientException.class, () -> breaker.call(down));
        assertEquals("ok", breaker.call(() -> "ok"));
        assertThrows(TransientException.class, () -> breaker.call(down));
        assertThrows(TransientException.class, () -> breaker.call(down));
        assertEquals(State.CLOSED, breaker.state(), "only consecutive failures count");
    }

    @Test
    void after_the_open_duration_one_trial_call_decides() {
        var clock = new TestClock();
        var breaker = new CircuitBreaker(1, Duration.ofSeconds(30), clock);
        Supplier<String> down = () -> { throw new TransientException("down"); };
        assertThrows(TransientException.class, () -> breaker.call(down));
        clock.advance(Duration.ofSeconds(29));
        assertEquals(State.OPEN, breaker.state());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(State.HALF_OPEN, breaker.state());
        assertThrows(TransientException.class, () -> breaker.call(down));
        assertEquals(State.OPEN, breaker.state(), "a failed trial opens it again");
        clock.advance(Duration.ofSeconds(30));
        assertEquals("ok", breaker.call(() -> "ok"));
        assertEquals(State.CLOSED, breaker.state());
    }

    @Test
    void retries_stop_as_soon_as_the_circuit_opens() {
        List<Duration> sleeps = new ArrayList<>();
        var client = new ResilientClient(RETRY, new CircuitBreaker(2, Duration.ofSeconds(30), new TestClock()), sleeps::add);
        var calls = new AtomicInteger();
        assertThrows(CircuitOpenException.class, () -> client.execute(failing(calls, 99, new TransientException("down"))));
        assertEquals(2, calls.get());
        assertEquals(2, sleeps.size());
    }
}
