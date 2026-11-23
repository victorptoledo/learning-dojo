package dojo.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class Stage2RangesAndRankTest {

    private static List<Long> keys(Iterator<Map.Entry<Long, String>> entries) {
        var keys = new ArrayList<Long>();
        entries.forEachRemaining(e -> keys.add(e.getKey()));
        return keys;
    }

    private static OrderedIndex<Long, String> tens() {
        var index = new OrderedIndex<Long, String>(Comparator.naturalOrder());
        for (long k : List.of(30L, 10L, 50L, 20L, 40L)) {
            index.put(k, "v" + k);
        }
        return index;
    }

    @Test
    void theReadmeExample() {
        var index = tens();
        assertEquals(List.of(20L, 30L, 40L), keys(index.range(20L, 45L)));
        assertEquals(List.of(10L, 20L), keys(index.range(null, 25L)));
        assertEquals(2, index.rank(30L));
        assertEquals(3, index.rank(35L));
        assertEquals(50L, index.select(4));
    }

    @Test
    void rangesCarryTheValuesAndRespectTheBounds() {
        var index = tens();
        var entries = index.range(30L, null);
        var first = entries.next();
        assertEquals(30L, first.getKey());
        assertEquals("v30", first.getValue());
        assertEquals(List.of(40L, 50L), keys(entries));
        assertEquals(List.of(10L, 20L, 30L, 40L, 50L), keys(index.range(null, null)));
        assertEquals(List.of(), keys(index.range(31L, 39L)));
        assertEquals(List.of(), keys(index.range(40L, 40L)));
        assertEquals(List.of(), keys(index.range(45L, 20L)), "from after to is empty");
        var empty = index.range(60L, null);
        assertFalse(empty.hasNext());
        assertThrows(NoSuchElementException.class, empty::next);
    }

    @Test
    void rankAndSelectAtTheEdges() {
        var index = tens();
        assertEquals(0, index.rank(10L));
        assertEquals(0, index.rank(-5L));
        assertEquals(5, index.rank(99L));
        assertEquals(10L, index.select(0));
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(5));
        assertThrows(IndexOutOfBoundsException.class, () -> index.select(-1));
        assertThrows(IllegalArgumentException.class, () -> index.rank(null));
        index.remove(30L);
        assertEquals(40L, index.select(2));
        assertEquals(2, index.rank(40L));
        var none = new OrderedIndex<Long, String>(Comparator.naturalOrder());
        assertEquals(0, none.rank(1L));
        assertThrows(IndexOutOfBoundsException.class, () -> none.select(0));
    }

    @Test
    void randomOperationsMatchATreeMap() {
        var random = new Random(111);
        var expected = new TreeMap<Long, String>();
        var index = new OrderedIndex<Long, String>(Comparator.naturalOrder());
        for (int step = 0; step < 20_000; step++) {
            long key = random.nextInt(3_000);
            if (random.nextInt(3) == 0) {
                assertEquals(expected.remove(key), index.remove(key));
            } else {
                assertEquals(expected.put(key, "s" + step), index.put(key, "s" + step));
            }
            long probe = random.nextInt(3_100) - 50;
            assertEquals(expected.headMap(probe).size(), index.rank(probe), "rank " + probe);
            if (!expected.isEmpty()) {
                int position = random.nextInt(expected.size());
                if (step % 10 == 0) {
                    assertEquals(new ArrayList<>(expected.keySet()).get(position), index.select(position), "select " + position);
                }
            }
            if (step % 50 == 0) {
                long from = random.nextInt(3_000);
                long to = from + random.nextInt(200);
                assertEquals(new ArrayList<>(expected.subMap(from, to).keySet()),
                        keys(index.range(from, to)), "range " + from + ".." + to);
                var values = new ArrayList<String>();
                index.range(from, to).forEachRemaining(e -> values.add(e.getValue()));
                assertEquals(new ArrayList<>(expected.subMap(from, to).values()), values);
            }
        }
    }

    @Test
    void rangesAndRanksStayWithinTheComparisonBudget() {
        var keys = new ArrayList<Long>();
        for (long i = 0; i < 100_000; i++) {
            keys.add(3 * i);
        }
        Collections.shuffle(keys, new Random(112));
        var counting = new Structures.CountingOrder();
        var index = new OrderedIndex<Long, Long>(counting);
        for (long k : keys) {
            index.put(k, k);
        }
        double log = Math.log(100_000) / Math.log(2);
        var random = new Random(113);
        counting.calls = 0;
        for (int i = 0; i < 10_000; i++) {
            long from = random.nextInt(300_000);
            var entries = index.range(from, null);
            for (int k = 0; k < 10 && entries.hasNext(); k++) {
                entries.next();
            }
        }
        double perRange = counting.calls / 10_000.0;
        assertTrue(perRange <= 3 * log + 2 * 10 + 10,
                String.format("%.1f comparisons to read 10 entries of a range, budget %.1f", perRange, 3 * log + 30));
        counting.calls = 0;
        for (int i = 0; i < 10_000; i++) {
            index.rank((long) random.nextInt(300_000));
        }
        double perRank = counting.calls / 10_000.0;
        assertTrue(perRank <= 3 * log + 10, String.format("%.1f comparisons per rank, budget %.1f", perRank, 3 * log + 10));
        counting.calls = 0;
        for (int i = 0; i < 1_000; i++) {
            index.select(random.nextInt(100_000));
        }
        assertEquals(0, counting.calls, "select needs no comparison");
    }

    @Test
    void rankAndSelectAreLogarithmic() {
        var random = new Random(114);
        var index = new OrderedIndex<Long, Long>(Comparator.naturalOrder());
        for (long i = 0; i < 200_000; i++) {
            index.put(random.nextLong(), i);
        }
        Structures.within(Duration.ofSeconds(5), () -> {
            long checksum = 0;
            for (int i = 0; i < 200_000; i++) {
                Structures.stopIfLate();
                Long key = index.select(random.nextInt(index.size()));
                int position = index.rank(key);
                assertEquals(key, index.select(position));
                checksum += position;
            }
            assertTrue(checksum > 0);
        }, "200,000 rank and select: is something counting from the start?");
        Structures.assertNoJdkSortedStructures(tens());
    }
}
