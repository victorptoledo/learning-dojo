package dojo.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dojo.shortener.Fakes.ManualClock;
import dojo.shortener.Fakes.MemoryAllocator;
import dojo.shortener.Fakes.MemoryStore;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class Stage2CacheTest {
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(1);

    private final ManualClock clock = new ManualClock();
    private final MemoryStore store = new MemoryStore();
    private final MemoryAllocator ids = new MemoryAllocator(1);

    private Shortener shortener(int capacity) {
        return new Shortener(store, ids, clock, new ShortenerConfig(100, 6, capacity, TTL, NEGATIVE_TTL));
    }

    /** A link another server created: it is in the store, not in this shortener's cache. */
    private void createdElsewhere(String code, String url) {
        store.links.put(code, url);
    }

    @Test
    void aSecondResolveIsAnsweredByTheCache() {
        var s = shortener(100);
        createdElsewhere("abc", "https://example.com/abc");
        assertEquals(Optional.of("https://example.com/abc"), s.resolve("abc"));
        assertEquals(Optional.of("https://example.com/abc"), s.resolve("abc"));
        assertEquals(Optional.of("https://example.com/abc"), s.resolve("abc"));
        assertEquals(1, store.gets.get());
        assertEquals(new CacheStats(2, 1, 0), s.stats());
    }

    @Test
    void unknownCodesAreCachedAsAbsent() {
        var s = shortener(100);
        assertEquals(Optional.empty(), s.resolve("nope"));
        assertEquals(Optional.empty(), s.resolve("nope"));
        assertEquals(1, store.gets.get(), "a scanner reaches the store once per code");
        assertEquals(new CacheStats(1, 1, 0), s.stats());
    }

    @Test
    void entriesLiveExactlyTheirTtl() {
        var s = shortener(100);
        createdElsewhere("abc", "https://example.com/abc");
        s.resolve("abc");
        clock.advance(TTL.minusSeconds(1));
        s.resolve("abc");
        assertEquals(1, store.gets.get(), "still good one second before the end");
        clock.advance(Duration.ofSeconds(1));
        s.resolve("abc");
        assertEquals(2, store.gets.get(), "read again when the TTL ends");
        assertEquals(new CacheStats(1, 2, 0), s.stats(), "an expired entry is a miss, not an eviction");
    }

    @Test
    void negativeEntriesHaveTheirOwnShorterTtl() {
        var s = shortener(100);
        s.resolve("later");
        createdElsewhere("later", "https://example.com/later");
        clock.advance(NEGATIVE_TTL.minusSeconds(1));
        assertEquals(Optional.empty(), s.resolve("later"), "still absent for this shortener");
        clock.advance(Duration.ofSeconds(1));
        assertEquals(Optional.of("https://example.com/later"), s.resolve("later"));
        assertEquals(2, store.gets.get());
    }

    @Test
    void aLinkCreatedHereIsCachedAtOnceAndReplacesANegativeEntry() {
        var s = shortener(100);
        String code = s.shorten("https://example.com/new");
        assertEquals(Optional.of("https://example.com/new"), s.resolve(code));
        assertEquals(0, store.gets.get(), "write-through: no read after a write");
        assertEquals(Optional.empty(), s.resolve("promo"));
        s.shorten("https://shop.example/promo", "promo");
        assertEquals(Optional.of("https://shop.example/promo"), s.resolve("promo"));
        assertEquals(1, store.gets.get());
    }

    @Test
    void aFullCacheDropsTheLeastRecentlyUsedEntry() {
        var s = shortener(3);
        createdElsewhere("aaa", "https://example.com/a");
        createdElsewhere("bbb", "https://example.com/b");
        createdElsewhere("ccc", "https://example.com/c");
        createdElsewhere("ddd", "https://example.com/d");
        s.resolve("aaa");
        s.resolve("bbb");
        s.resolve("ccc");
        s.resolve("aaa"); // aaa is now the most recent; bbb the least
        s.resolve("ddd"); // pushes bbb out
        assertEquals(4, store.gets.get());
        s.resolve("aaa");
        s.resolve("ccc");
        s.resolve("ddd");
        assertEquals(4, store.gets.get(), "aaa, ccc and ddd stayed");
        s.resolve("bbb");
        assertEquals(5, store.gets.get(), "bbb was evicted");
        assertEquals(new CacheStats(4, 5, 2), s.stats());
    }

    @Test
    void negativeEntriesTakeRoomToo() {
        var s = shortener(2);
        createdElsewhere("hot", "https://example.com/hot");
        s.resolve("hot");
        s.resolve("x1");
        s.resolve("x2"); // the cache holds 2: "hot" is the least recently used and goes
        s.resolve("hot");
        assertEquals(4, store.gets.get());
        assertEquals(2, s.stats().evictions());
    }

    @Test
    void aWarmCacheKeepsTheStoreOutOfTheRedirectPath() {
        var s = shortener(1_000);
        for (int i = 0; i < 1_000; i++) {
            createdElsewhere("link" + i, "https://example.com/" + i);
        }
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 1_000; i++) {
                s.resolve("link" + i);
            }
        }
        assertEquals(1_000, store.gets.get());
        assertEquals(new CacheStats(19_000, 1_000, 0), s.stats());
    }

    @Test
    void deleteRemovesTheLinkFromTheStoreAndThisCache() {
        var s = shortener(100);
        String code = s.shorten("https://example.com/gone");
        s.resolve(code);
        assertTrue(s.delete(code));
        assertEquals(Optional.empty(), s.resolve(code));
        assertFalse(store.links.containsKey(code));
        assertFalse(s.delete(code), "deleting twice");
        assertFalse(s.delete("never"));
    }

    @Test
    void anotherServerSeesADeleteWithinTheTtl() {
        var a = shortener(100);
        var b = shortener(100);
        String code = a.shorten("https://example.com/gone");
        assertEquals(Optional.of("https://example.com/gone"), b.resolve(code));
        assertTrue(a.delete(code));
        clock.advance(TTL.minusSeconds(1));
        assertEquals(Optional.of("https://example.com/gone"), b.resolve(code), "b's cache is stale, for a bounded time");
        clock.advance(Duration.ofSeconds(1));
        assertEquals(Optional.empty(), b.resolve(code));
    }
}
