package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FencedLeaseGradingTest {
    private final TestClock clock = new TestClock();
    private final LeaseManager leases = new LeaseManager(Duration.ofSeconds(10), clock);

    @Test
    void a_lease_blocks_everyone_until_it_expires() {
        var a = leases.tryAcquire("a").orElseThrow();
        assertEquals(clock.instant().plusSeconds(10), a.expiresAt());
        assertTrue(leases.tryAcquire("a").isEmpty(), "even the holder must renew, not acquire");
        clock.advance(Duration.ofSeconds(10).minusMillis(1));
        assertTrue(leases.tryAcquire("b").isEmpty());
        assertEquals(Optional.of(a), leases.current());
        clock.advance(Duration.ofMillis(1));
        assertEquals(Optional.empty(), leases.current(), "expired at exactly expiresAt");
        var b = leases.tryAcquire("b").orElseThrow();
        assertEquals(2, b.token());
    }

    @Test
    void renewing_extends_the_lease_and_keeps_the_token() {
        var a = leases.tryAcquire("a").orElseThrow();
        clock.advance(Duration.ofSeconds(6));
        var renewed = leases.renew(a).orElseThrow();
        assertEquals(1, renewed.token());
        assertEquals(clock.instant().plusSeconds(10), renewed.expiresAt());
        clock.advance(Duration.ofSeconds(6));
        assertTrue(leases.tryAcquire("b").isEmpty(), "the renewal is still live");
        assertTrue(leases.renew(a).isPresent(), "renewing with the earlier copy of the same lease works");
    }

    @Test
    void an_expired_or_superseded_lease_cannot_be_renewed() {
        var a = leases.tryAcquire("a").orElseThrow();
        clock.advance(Duration.ofSeconds(10));
        assertTrue(leases.renew(a).isEmpty(), "too late");
        var b = leases.tryAcquire("b").orElseThrow();
        assertTrue(leases.renew(a).isEmpty(), "a's token is no longer the live one");
        assertTrue(leases.renew(b).isPresent());
    }

    @Test
    void releasing_lets_the_next_owner_in_with_a_higher_token() {
        var a = leases.tryAcquire("a").orElseThrow();
        assertTrue(leases.release(a));
        assertFalse(leases.release(a), "already released");
        var b = leases.tryAcquire("b").orElseThrow();
        assertEquals(2, b.token());
        assertFalse(leases.release(a), "a stale lease cannot release the live one");
        assertEquals(Optional.of(b), leases.current());
        assertTrue(leases.renew(a).isEmpty());
    }

    @Test
    void tokens_strictly_increase_across_every_handover() {
        long previous = 0;
        for (int i = 0; i < 20; i++) {
            var lease = leases.tryAcquire("w" + i).orElseThrow();
            assertTrue(lease.token() > previous);
            previous = lease.token();
            if (i % 2 == 0) {
                leases.release(lease);
            } else {
                clock.advance(Duration.ofSeconds(11));
            }
        }
        assertEquals(20, previous);
    }

    @Test
    void the_store_rejects_a_holder_that_woke_up_after_losing_its_lease() {
        var store = new FencedStore();
        var a = leases.tryAcquire("a").orElseThrow();
        store.write("report", "A started", a.token());
        clock.advance(Duration.ofSeconds(15));
        var b = leases.tryAcquire("b").orElseThrow();
        store.write("report", "B", b.token());
        assertThrows(StaleTokenException.class, () -> store.write("report", "A", a.token()));
        assertEquals(Optional.of("B"), store.read("report"));
        assertEquals(2, store.highestToken());
    }

    @Test
    void the_holder_may_write_many_times_and_newer_tokens_always_pass() {
        var store = new FencedStore();
        assertEquals(0, store.highestToken());
        store.write("k", "1", 5);
        store.write("k", "2", 5);
        store.write("other", "x", 7);
        assertEquals(Optional.of("2"), store.read("k"));
        assertThrows(StaleTokenException.class, () -> store.write("k", "3", 6));
        assertEquals(Optional.of("2"), store.read("k"), "a rejected write changes nothing");
        assertEquals(Optional.empty(), store.read("missing"));
    }

    @Test
    void it_is_safe_to_act_only_before_the_margin() {
        var a = leases.tryAcquire("a").orElseThrow();
        var margin = Duration.ofSeconds(2);
        assertTrue(leases.isSafeToAct(a, margin));
        clock.advance(Duration.ofSeconds(8).minusMillis(1));
        assertTrue(leases.isSafeToAct(a, margin));
        clock.advance(Duration.ofMillis(1));
        assertFalse(leases.isSafeToAct(a, margin), "inside the safety margin");
        var renewed = leases.renew(a).orElseThrow();
        assertTrue(leases.isSafeToAct(renewed, margin));
        leases.release(renewed);
        assertFalse(leases.isSafeToAct(renewed, margin), "released");
    }

    @Test
    void only_one_of_many_concurrent_callers_gets_the_lease() throws Exception {
        var winners = new AtomicInteger();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 16; i++) {
                String owner = "w" + i;
                executor.submit(() -> {
                    start.await();
                    if (leases.tryAcquire(owner).isPresent()) {
                        winners.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }
        assertEquals(1, winners.get());
        assertEquals(1, leases.current().orElseThrow().token());
    }

    @Test
    void a_lease_must_last_a_positive_time() {
        assertThrows(IllegalArgumentException.class, () -> new LeaseManager(Duration.ZERO, clock));
        assertThrows(IllegalArgumentException.class, () -> new LeaseManager(Duration.ofSeconds(-1), clock));
    }
}
