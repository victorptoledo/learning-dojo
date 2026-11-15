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

class LruTtlCacheGradingTest {
    private final TestClock clock = new TestClock();

    private LruTtlCache<String, Integer> cache(int capacity) {
        return new LruTtlCache<>(capacity, Duration.ofSeconds(60), clock);
    }

    @Test
    void reads_count_hits_and_misses() {
        var cache = cache(10);
        cache.put("a", 1);
        cache.get("a");
        cache.get("a");
        cache.get("missing");
        assertEquals(new CacheStats(2, 1, 0, 0), cache.stats());
    }

    @Test
    void a_read_makes_an_entry_recent() {
        var cache = cache(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.get("a");
        cache.put("d", 4);
        assertEquals(Optional.empty(), cache.get("b"), "b was the least recently used");
        assertEquals(Optional.of(1), cache.get("a"));
        assertEquals(1, cache.stats().evictions());
    }

    @Test
    void overwriting_a_key_refreshes_it_and_evicts_nothing() {
        var cache = cache(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("a", 10);
        assertEquals(0, cache.stats().evictions());
        cache.put("c", 3);
        assertEquals(Optional.empty(), cache.get("b"));
        assertEquals(Optional.of(10), cache.get("a"));
        assertEquals(2, cache.size());
    }

    @Test
    void an_entry_expires_exactly_at_its_ttl() {
        var cache = cache(10);
        cache.put("a", 1);
        clock.advance(Duration.ofSeconds(60).minusMillis(1));
        assertEquals(Optional.of(1), cache.get("a"));
        clock.advance(Duration.ofMillis(1));
        assertEquals(Optional.empty(), cache.get("a"));
        assertEquals(new CacheStats(1, 1, 0, 1), cache.stats());
    }

    @Test
    void entries_can_have_their_own_ttl() {
        var cache = cache(10);
        cache.put("short", 1, Duration.ofSeconds(5));
        cache.put("long", 2);
        clock.advance(Duration.ofSeconds(5));
        assertEquals(Optional.empty(), cache.get("short"));
        assertEquals(Optional.of(2), cache.get("long"));
        cache.put("long", 3, Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(1));
        assertEquals(Optional.empty(), cache.get("long"), "overwriting replaces the expiry too");
    }

    @Test
    void a_full_cache_drops_expired_entries_before_live_ones() {
        var cache = cache(3);
        cache.put("old", 1, Duration.ofSeconds(1));
        cache.put("b", 2);
        cache.put("c", 3);
        clock.advance(Duration.ofSeconds(2));
        cache.get("b");
        cache.get("c");
        cache.put("d", 4);
        assertEquals(0, cache.stats().evictions(), "the expired entry made room");
        assertEquals(1, cache.stats().expirations());
        assertEquals(3, cache.size());
        assertTrue(cache.get("b").isPresent() && cache.get("c").isPresent() && cache.get("d").isPresent());
    }

    @Test
    void size_counts_only_live_entries() {
        var cache = cache(10);
        cache.put("a", 1, Duration.ofSeconds(1));
        cache.put("b", 2, Duration.ofSeconds(10));
        cache.put("c", 3, Duration.ofSeconds(10));
        assertEquals(3, cache.size());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(2, cache.size());
        assertEquals(1, cache.stats().expirations());
    }

    @Test
    void get_or_load_loads_a_missing_key_once_and_caches_it() {
        var cache = cache(10);
        var loads = new AtomicInteger();
        assertEquals(42, cache.getOrLoad("answer", k -> { loads.incrementAndGet(); return 42; }));
        assertEquals(42, cache.getOrLoad("answer", k -> { loads.incrementAndGet(); return 0; }));
        assertEquals(1, loads.get());
        assertEquals(new CacheStats(1, 1, 0, 0), cache.stats());
        clock.advance(Duration.ofSeconds(60));
        assertEquals(7, cache.getOrLoad("answer", k -> 7), "an expired entry is loaded again");
    }

    @Test
    void concurrent_misses_on_one_key_cause_a_single_load() throws Exception {
        var cache = cache(10);
        var loads = new AtomicInteger();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 16; i++) {
                executor.submit(() -> {
                    start.await();
                    return cache.getOrLoad("hot", k -> {
                        loads.incrementAndGet();
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return 1;
                    });
                });
            }
            start.countDown();
        }
        assertEquals(1, loads.get(), "the loader ran more than once for the same key");
        assertEquals(15, cache.stats().hits());
    }

    @Test
    void invalidate_removes_a_key() {
        var cache = cache(10);
        cache.put("a", 1);
        assertTrue(cache.invalidate("a"));
        assertFalse(cache.invalidate("a"));
        assertEquals(Optional.empty(), cache.get("a"));
        cache.put("b", 2, Duration.ofSeconds(1));
        clock.advance(Duration.ofSeconds(1));
        assertFalse(cache.invalidate("b"), "an expired entry was not live");
    }

    @Test
    void impossible_caches_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new LruTtlCache<String, String>(0, Duration.ofSeconds(1), clock));
        assertThrows(IllegalArgumentException.class, () -> new LruTtlCache<String, String>(1, Duration.ZERO, clock));
        var cache = cache(1);
        assertThrows(IllegalArgumentException.class, () -> cache.put("a", 1, Duration.ofSeconds(-1)));
    }
}
