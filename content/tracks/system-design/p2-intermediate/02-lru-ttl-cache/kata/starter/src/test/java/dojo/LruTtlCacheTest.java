package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LruTtlCacheTest {
    @Test
    void a_value_put_can_be_read() {
        var cache = new LruTtlCache<String, String>(10, Duration.ofMinutes(5), new TestClock());
        cache.put("user:1", "Ada");
        assertEquals(Optional.of("Ada"), cache.get("user:1"));
    }

    @Test
    void the_least_recently_used_entry_is_evicted() {
        var cache = new LruTtlCache<String, Integer>(2, Duration.ofMinutes(5), new TestClock());
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        assertEquals(Optional.empty(), cache.get("a"));
    }
}
