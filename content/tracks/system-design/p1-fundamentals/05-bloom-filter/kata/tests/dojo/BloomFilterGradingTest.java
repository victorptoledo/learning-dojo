package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BloomFilterGradingTest {
    @Test
    void the_bits_set_are_the_double_hashing_positions() {
        var filter = new BloomFilter(1000, 3);
        filter.add("x");
        long[] h = Hashing.hash128("x");
        var expected = new TreeSet<Integer>();
        for (int i = 0; i < 3; i++) {
            expected.add((int) Math.floorMod(h[0] + i * h[1], 1000L));
        }
        assertEquals(expected.size(), filter.bitsSet());
        var other = new BloomFilter(1000, 3);
        other.add("y");
        assertEquals(1, filter.itemsAdded());
        assertTrue(filter.mightContain("x"));
    }

    @Test
    void there_are_no_false_negatives() {
        var filter = BloomFilter.forCapacity(10_000, 0.01);
        for (int i = 0; i < 10_000; i++) {
            filter.add("in-" + i);
        }
        for (int i = 0; i < 10_000; i++) {
            assertTrue(filter.mightContain("in-" + i), "in-" + i + " was added");
        }
    }

    @Test
    void the_false_positive_rate_stays_close_to_the_target() {
        var filter = BloomFilter.forCapacity(10_000, 0.01);
        for (int i = 0; i < 10_000; i++) {
            filter.add("in-" + i);
        }
        int falsePositives = 0;
        for (int i = 0; i < 100_000; i++) {
            if (filter.mightContain("out-" + i)) {
                falsePositives++;
            }
        }
        double rate = falsePositives / 100_000.0;
        assertTrue(rate < 0.02, "false-positive rate " + rate);
        assertTrue(rate > 0.002, "a rate of " + rate + " means the filter is far bigger or uses other positions");
    }

    @Test
    void capacity_sizing_follows_the_formulas() {
        var small = BloomFilter.forCapacity(1000, 0.01);
        assertEquals(9586, small.bitCount());
        assertEquals(7, small.hashCount());
        var strict = BloomFilter.forCapacity(1000, 0.001);
        assertEquals(14378, strict.bitCount());
        assertEquals(10, strict.hashCount());
        var loose = BloomFilter.forCapacity(1, 0.5);
        assertEquals(2, loose.bitCount());
        assertEquals(1, loose.hashCount());
    }

    @Test
    void the_expected_rate_grows_with_the_items() {
        var filter = BloomFilter.forCapacity(1000, 0.01);
        assertEquals(0.0, filter.expectedFalsePositiveRate());
        for (int i = 0; i < 1000; i++) {
            filter.add("item-" + i);
        }
        double rate = filter.expectedFalsePositiveRate();
        assertTrue(rate > 0.008 && rate < 0.012, "expected about 1 %, got " + rate);
        for (int i = 1000; i < 3000; i++) {
            filter.add("item-" + i);
        }
        assertTrue(filter.expectedFalsePositiveRate() > 0.1, "an overfilled filter degrades");
        assertEquals(3000, filter.itemsAdded());
    }

    @Test
    void adding_the_same_item_again_sets_no_new_bits() {
        var filter = new BloomFilter(4096, 5);
        filter.add("same");
        int bits = filter.bitsSet();
        filter.add("same");
        assertEquals(bits, filter.bitsSet());
        assertEquals(2, filter.itemsAdded());
        assertFalse(new BloomFilter(4096, 5).mightContain("same"));
    }

    @Test
    void impossible_filters_are_refused() {
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(0, 3));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(100, 0));
        assertThrows(IllegalArgumentException.class, () -> BloomFilter.forCapacity(0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> BloomFilter.forCapacity(100, 0));
        assertThrows(IllegalArgumentException.class, () -> BloomFilter.forCapacity(100, 1));
        assertThrows(IllegalArgumentException.class, () -> BloomFilter.forCapacity(Long.MAX_VALUE / 4, 0.01));
    }
}
