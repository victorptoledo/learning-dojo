package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ChainedHashMapGradingTest {

    /** A key whose hash code is always the same: only equals tells two keys apart. */
    private record BadKey(int id) {
        @Override
        public int hashCode() {
            return 42;
        }
    }

    @Test
    void random_operations_match_java_util_hash_map() {
        var random = new Random(11);
        var expected = new HashMap<String, Integer>();
        var actual = new ChainedHashMap<String, Integer>();
        for (int step = 0; step < 50_000; step++) {
            String key = "k" + random.nextInt(5_000);
            switch (random.nextInt(4)) {
                case 0, 1 -> assertEquals(expected.put(key, step), actual.put(key, step));
                case 2 -> assertEquals(expected.remove(key), actual.remove(key));
                default -> {
                    assertEquals(expected.get(key), actual.get(key));
                    assertEquals(expected.containsKey(key), actual.containsKey(key));
                }
            }
            assertEquals(expected.size(), actual.size());
        }
        assertEquals(expected.keySet(), new HashSet<>(actual.keys()));
    }

    @Test
    void null_keys_are_refused_and_null_values_are_kept() {
        var map = new ChainedHashMap<String, String>();
        assertThrows(IllegalArgumentException.class, () -> map.put(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> map.get(null));
        assertThrows(IllegalArgumentException.class, () -> map.remove(null));
        assertTrue(map.isEmpty());
        assertNull(map.put("k", null));
        assertTrue(map.containsKey("k"));
        assertNull(map.get("k"));
        assertFalse(map.containsKey("other"));
        assertNull(map.remove("other"));
        assertEquals(1, map.size());
    }

    @Test
    void colliding_hash_codes_are_told_apart_by_equals() {
        var map = new ChainedHashMap<BadKey, Integer>();
        for (int i = 0; i < 2_000; i++) {
            map.put(new BadKey(i), i);
        }
        assertEquals(2_000, map.size());
        assertEquals(2_000, map.longestChain(), "every key shares one bucket");
        for (int i = 0; i < 2_000; i += 7) {
            assertEquals(i, map.get(new BadKey(i)));
        }
        assertEquals(10, map.remove(new BadKey(10)));
        assertNull(map.get(new BadKey(10)));
        assertEquals(1_999, map.size());
    }

    @Test
    void the_table_doubles_at_a_load_factor_of_three_quarters() {
        var map = new ChainedHashMap<Integer, Integer>();
        for (int i = 1; i <= 12; i++) {
            map.put(i, i);
        }
        assertEquals(16, map.bucketCount(), "12 of 16 is exactly 0.75");
        map.put(13, 13);
        assertEquals(32, map.bucketCount());
        map.put(13, 14);
        assertEquals(13, map.size(), "replacing does not add");
        for (int i = 0; i < 100_000; i++) {
            map.put(1_000 + i, i);
            assertTrue(map.size() <= map.bucketCount() * 0.75);
        }
        assertEquals(262_144, map.bucketCount());
        int buckets = map.bucketCount();
        assertEquals(1, Integer.bitCount(buckets), "a power of two");
    }

    @Test
    void keys_that_differ_only_in_high_bits_are_spread() {
        var map = new ChainedHashMap<Integer, Integer>();
        assertTimeoutPreemptively(Duration.ofSeconds(4), () -> {
            for (int i = 0; i < 65_536; i++) {
                map.put(i << 16, i);
            }
            for (int i = 0; i < 65_536; i++) {
                assertEquals(i, map.get(i << 16));
            }
        });
        assertTrue(map.longestChain() <= 16, "longest chain " + map.longestChain());
    }

    @Test
    void a_million_operations_are_fast() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var map = new ChainedHashMap<Integer, Integer>();
            for (int i = 0; i < 1_000_000; i++) {
                map.put(i * 31, i);
            }
            long sum = 0;
            for (int i = 0; i < 1_000_000; i++) {
                sum += map.get(i * 31);
            }
            assertEquals(499_999_500_000L, sum);
        });
    }
}
