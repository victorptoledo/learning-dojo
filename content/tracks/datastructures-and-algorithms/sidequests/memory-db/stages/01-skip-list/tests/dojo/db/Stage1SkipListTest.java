package dojo.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class Stage1SkipListTest {

    private static double log2(int n) {
        return Math.log(n) / Math.log(2);
    }

    @Test
    void aSmallMap() {
        var index = new OrderedIndex<String, Integer>(Comparator.naturalOrder());
        assertNull(index.first());
        assertNull(index.last());
        assertNull(index.put("m", 1));
        assertNull(index.put("c", 2));
        assertNull(index.put("x", 3));
        assertEquals(1, index.put("m", 10));
        assertEquals(3, index.size());
        assertEquals(10, index.get("m"));
        assertNull(index.get("a"));
        assertEquals("c", index.first());
        assertEquals("x", index.last());
        assertEquals("m", index.floor("n"));
        assertEquals("m", index.floor("m"));
        assertNull(index.floor("b"));
        assertEquals("x", index.ceiling("n"));
        assertNull(index.ceiling("y"));
        assertEquals(2, index.remove("c"));
        assertNull(index.remove("c"));
        assertEquals("m", index.first());
        assertEquals(2, index.size());
    }

    @Test
    void theComparatorDecidesTheOrder() {
        var index = new OrderedIndex<String, String>(Comparator.comparing(String::length).thenComparing(Comparator.reverseOrder()));
        for (String w : List.of("bb", "a", "ccc", "zz", "b")) {
            index.put(w, w.toUpperCase());
        }
        assertEquals("b", index.first(), "the shortest, and then the last in reverse order");
        assertEquals("ccc", index.last());
        assertEquals("zz", index.ceiling("zz"));
        assertEquals("bb", index.floor("ba"), "ba sorts after bb in reverse order");
    }

    @Test
    void badArgumentsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new OrderedIndex<String, String>(null));
        var index = new OrderedIndex<String, String>(Comparator.naturalOrder());
        assertThrows(IllegalArgumentException.class, () -> index.put(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> index.get(null));
        assertThrows(IllegalArgumentException.class, () -> index.remove(null));
        assertThrows(IllegalArgumentException.class, () -> index.floor(null));
        assertThrows(IllegalArgumentException.class, () -> index.ceiling(null));
        assertNull(index.put("k", null), "a null value is a value");
        assertEquals(1, index.size());
    }

    @Test
    void randomOperationsMatchATreeMap() {
        var random = new Random(101);
        var expected = new TreeMap<Long, Long>();
        var index = new OrderedIndex<Long, Long>(Comparator.naturalOrder());
        for (int step = 0; step < 30_000; step++) {
            long key = random.nextInt(2_000);
            switch (random.nextInt(3)) {
                case 0 -> assertEquals(expected.remove(key), index.remove(key), "remove " + key);
                default -> assertEquals(expected.put(key, (long) step), index.put(key, (long) step), "put " + key);
            }
            long probe = random.nextInt(2_100) - 50;
            assertEquals(expected.get(probe), index.get(probe));
            assertEquals(expected.floorKey(probe), index.floor(probe), "floor " + probe);
            assertEquals(expected.ceilingKey(probe), index.ceiling(probe), "ceiling " + probe);
            assertEquals(expected.size(), index.size());
            if (step % 100 == 0) {
                assertEquals(expected.isEmpty() ? null : expected.firstKey(), index.first());
                assertEquals(expected.isEmpty() ? null : expected.lastKey(), index.last());
            }
        }
    }

    @Test
    void noJdkSortedCollectionInside() {
        var index = new OrderedIndex<Long, String>(Comparator.naturalOrder());
        for (long i = 0; i < 1_000; i++) {
            index.put(i * 7 % 1_000, "v" + i);
        }
        Structures.assertNoJdkSortedStructures(index);
    }

    private static void checkBudget(List<Long> keys, String order) {
        var counting = new Structures.CountingOrder();
        var index = new OrderedIndex<Long, Long>(counting);
        int n = keys.size();
        double budget = 3 * log2(n) + 10;
        for (long key : keys) {
            Structures.stopIfLate();
            index.put(key, key);
        }
        double perPut = (double) counting.calls / n;
        assertTrue(perPut <= budget, String.format("%.1f comparisons per put with %s keys, budget %.1f", perPut, order, budget));
        var random = new Random(102);
        counting.calls = 0;
        for (int i = 0; i < n; i++) {
            Structures.stopIfLate();
            long probe = random.nextInt(2 * n);
            index.get(probe);
            index.floor(probe);
            index.ceiling(probe);
        }
        double perQuery = (double) counting.calls / (3.0 * n);
        assertTrue(perQuery <= budget,
                String.format("%.1f comparisons per get, floor or ceiling with %s keys, budget %.1f", perQuery, order, budget));
        counting.calls = 0;
        for (int i = 0; i < n; i += 2) {
            Structures.stopIfLate();
            index.remove(keys.get(i));
        }
        double perRemove = (double) counting.calls / (n / 2.0);
        assertTrue(perRemove <= budget, String.format("%.1f comparisons per remove, budget %.1f", perRemove, budget));
        assertEquals(n / 2, index.size());
    }

    @Test
    void randomKeysStayWithinTheComparisonBudget() {
        var keys = new ArrayList<Long>();
        for (long i = 0; i < 100_000; i++) {
            keys.add(2 * i);
        }
        Collections.shuffle(keys, new Random(103));
        Structures.within(Duration.ofSeconds(6), () -> checkBudget(keys, "random"));
    }

    @Test
    void sortedKeysStayWithinTheComparisonBudget() {
        var keys = new ArrayList<Long>();
        for (long i = 0; i < 100_000; i++) {
            keys.add(2 * i);
        }
        Structures.within(Duration.ofSeconds(6), () -> checkBudget(keys, "sorted"),
                "keys inserted in order: does the structure become a list?");
    }

    @Test
    void manyInsertionsAreFast() {
        var index = new OrderedIndex<Long, Long>(Comparator.naturalOrder());
        Structures.within(Duration.ofSeconds(6), () -> {
            // descending: in a sorted array every insertion would shift the whole array
            for (long i = 500_000; i > 0; i--) {
                Structures.stopIfLate();
                index.put(i, i);
            }
            assertEquals(500_000, index.size());
            assertEquals(1L, index.first());
            assertEquals(500_000L, index.last());
        }, "500,000 insertions at the front: is each one shifting an array?");
    }
}
