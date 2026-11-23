package dojo.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage4BitmapTest {

    @Test
    void bitmapsAnswerAndCount() {
        var store = new CountingStore();
        var table = new Table(List.of("city", "status"), store);
        table.insert(1, 0);
        table.insert(2, 1);
        table.insert(1, 1);
        table.createBitmapIndex("city");
        table.createBitmapIndex("status");
        table.insert(3, 1);
        table.insert(1, 1);
        store.reads = 0;
        assertEquals(List.of(2, 4), table.query(List.of(new Predicate.Eq("city", 1), new Predicate.Eq("status", 1))));
        assertEquals(2, table.count(List.of(new Predicate.Eq("city", 1), new Predicate.Eq("status", 1))));
        assertEquals(4, table.count(List.of(new Predicate.In("city", Set.of(1L, 3L)))));
        assertEquals(List.of(1, 3), table.query(List.of(new Predicate.Range("city", 2, 10))));
        assertEquals(0, table.count(List.of(new Predicate.Eq("city", 9))));
        assertEquals(5, table.count(List.of()));
        assertEquals(0, store.reads, "every predicate is on a bitmap: nothing to read");
        table.delete(2);
        assertEquals(1, table.count(List.of(new Predicate.Eq("city", 1), new Predicate.Eq("status", 1))));
        assertEquals(List.of(0, 4), table.query(List.of(new Predicate.Eq("city", 1))));
    }

    @Test
    void badBitmapIndexesAreRefused() {
        var table = new Table(List.of("a"), new CountingStore());
        assertThrows(IllegalArgumentException.class, () -> table.createBitmapIndex("b"));
        table.createBitmapIndex("a");
        assertThrows(IllegalArgumentException.class, () -> table.createBitmapIndex("a"));
        table.createIndex("a");
        assertThrows(IllegalArgumentException.class, () -> table.count(List.of(new Predicate.Eq("b", 1))));
        assertThrows(IllegalArgumentException.class, () -> table.count(null));
    }

    @Test
    void orderedAndBitmapIndexesCombineWithoutReads() {
        var store = new CountingStore();
        var table = Stage3TableTest.people(store, 20_000, 131);
        table.createBitmapIndex("city");
        table.createIndex("age");
        var where = List.<Predicate>of(new Predicate.Eq("city", 4), new Predicate.Range("age", 30, 40));
        var expected = Stage3TableTest.scan(store, 20_000, List.of(), where);
        store.reads = 0;
        assertEquals(expected, table.query(where));
        assertEquals(expected.size(), table.count(where));
        assertEquals(0, store.reads, "both predicates are indexed: no row is read");
        var withScore = new ArrayList<>(where);
        withScore.add(new Predicate.Range("score", 0, 50_000));
        store.reads = 0;
        var half = table.query(withScore);
        assertTrue(store.reads <= expected.size(), store.reads + " reads; only the " + expected.size() + " rows both indexes allow may be read");
        assertEquals(Stage3TableTest.scan(store, 20_000, List.of(), withScore), half);
    }

    @Test
    void randomQueriesAndCountsMatchAScan() {
        var random = new Random(132);
        for (int round = 0; round < 20; round++) {
            var store = new CountingStore();
            int rows = 400;
            var table = Stage3TableTest.people(store, rows, 200 + round);
            table.createBitmapIndex("city");
            if (random.nextBoolean()) {
                table.createBitmapIndex("age");
            } else {
                table.createIndex("age");
            }
            var deleted = new ArrayList<Integer>();
            for (int i = 0; i < 40; i++) {
                int row = random.nextInt(rows);
                if (!deleted.contains(row)) {
                    table.delete(row);
                    deleted.add(row);
                }
            }
            for (int i = 0; i < 5; i++) {
                rows++; // the store gives the next id, the old row count
                table.insert(rows, random.nextInt(10), 18 + random.nextInt(60), random.nextInt(100_000));
            }
            for (int query = 0; query < 40; query++) {
                var where = new ArrayList<Predicate>();
                if (random.nextBoolean()) {
                    where.add(new Predicate.In("city", Set.copyOf(List.of((long) random.nextInt(10), (long) random.nextInt(10)))));
                }
                if (random.nextBoolean()) {
                    long from = 18 + random.nextInt(60);
                    where.add(new Predicate.Range("age", from, from + random.nextInt(15)));
                }
                if (random.nextInt(3) == 0) {
                    where.add(new Predicate.Range("score", 0, random.nextInt(100_000)));
                }
                var expected = Stage3TableTest.scan(store, rows, deleted, where);
                assertEquals(expected, table.query(where), "query " + where);
                assertEquals(expected.size(), table.count(where), "count " + where);
            }
        }
    }

    @Test
    void countsOverHalfAMillionRowsUseWordOperations() {
        var store = new CountingStore();
        var table = Stage3TableTest.people(store, 500_000, 133);
        table.createBitmapIndex("city");
        table.createBitmapIndex("age");
        var random = new Random(134);
        store.reads = 0;
        Structures.within(Duration.ofSeconds(8), () -> {
            long total = 0;
            for (int i = 0; i < 2_000; i++) {
                Structures.stopIfLate();
                long from = 18 + random.nextInt(40);
                total += table.count(List.of(
                        new Predicate.Eq("city", random.nextInt(10)),
                        new Predicate.Range("age", from, from + 20),
                        new Predicate.In("city", Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L))));
            }
            assertTrue(total > 0);
        }, "2,000 counts on 500,000 rows: one bit per row, 64 rows per word operation");
        assertEquals(0, store.reads, "the counts read no row");
        var smallStore = new CountingStore();
        var small = Stage3TableTest.people(smallStore, 300, 2);
        small.createBitmapIndex("city");
        Structures.assertNoJdkSortedStructures(small, smallStore);
    }
}
