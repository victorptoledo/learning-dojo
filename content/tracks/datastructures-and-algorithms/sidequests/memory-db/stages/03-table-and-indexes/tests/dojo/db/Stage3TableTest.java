package dojo.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage3TableTest {

    static final List<String> COLUMNS = List.of("id", "city", "age", "score");

    /** A table of people: city 0..9, age 18..77, score 0..99,999. */
    static Table people(CountingStore store, int rows, long seed) {
        var table = new Table(COLUMNS, store);
        var random = new Random(seed);
        for (int i = 0; i < rows; i++) {
            table.insert(i, random.nextInt(10), 18 + random.nextInt(60), random.nextInt(100_000));
        }
        return table;
    }

    /** The answer by reading every row of the store, without the table. */
    static List<Integer> scan(CountingStore store, int rows, List<Integer> deleted, List<Predicate> where) {
        var result = new ArrayList<Integer>();
        for (int row = 0; row < rows; row++) {
            if (deleted.contains(row)) {
                continue;
            }
            long[] values = store.read(row);
            boolean all = true;
            for (Predicate p : where) {
                long v = values[COLUMNS.indexOf(p.column())];
                all &= switch (p) {
                    case Predicate.Eq eq -> v == eq.value();
                    case Predicate.Range r -> r.from() <= v && v < r.to();
                    case Predicate.In in -> in.values().contains(v);
                };
            }
            if (all) {
                result.add(row);
            }
        }
        return result;
    }

    @Test
    void insertGetDeleteAndQuery() {
        var store = new CountingStore();
        var table = new Table(List.of("a", "b"), store);
        assertEquals(0, table.insert(5, 50));
        assertEquals(1, table.insert(6, 60));
        assertEquals(2, table.insert(5, 70));
        assertArrayEquals(new long[] {6, 60}, table.get(1));
        assertEquals(3, table.size());
        assertEquals(List.of(0, 2), table.query(List.of(new Predicate.Eq("a", 5))));
        assertEquals(List.of(2), table.query(List.of(new Predicate.Eq("a", 5), new Predicate.Range("b", 55, 100))));
        assertEquals(List.of(0, 1, 2), table.query(List.of()));
        assertEquals(List.of(1, 2), table.query(List.of(new Predicate.In("b", Set.of(60L, 70L, 80L)))));
        assertEquals(List.of(), table.query(List.of(new Predicate.Range("b", 70, 70))));
        assertTrue(table.delete(0));
        assertFalse(table.delete(0));
        assertFalse(table.delete(99));
        assertEquals(2, table.size());
        assertEquals(List.of(2), table.query(List.of(new Predicate.Eq("a", 5))));
        assertThrows(IllegalArgumentException.class, () -> table.get(0));
        assertEquals(3, table.insert(1, 1), "ids come from the store and are never reused");
    }

    @Test
    void indexesFollowInsertsAndDeletes() {
        var store = new CountingStore();
        var table = new Table(List.of("a", "b"), store);
        table.insert(5, 50);
        table.insert(6, 60);
        table.createIndex("a");
        table.insert(5, 70);
        assertEquals(List.of(0, 2), table.query(List.of(new Predicate.Eq("a", 5))));
        table.delete(0);
        assertEquals(List.of(2), table.query(List.of(new Predicate.Eq("a", 5))));
        assertEquals(List.of(1, 2), table.query(List.of(new Predicate.Range("a", 5, 7))));
        assertEquals(List.of(1), table.query(List.of(new Predicate.In("a", Set.of(6L, 9L)))));
        assertEquals(List.of(), table.query(List.of(new Predicate.In("a", Set.of()))));
    }

    @Test
    void badArgumentsAreRefused() {
        var store = new CountingStore();
        assertThrows(IllegalArgumentException.class, () -> new Table(List.of(), store));
        assertThrows(IllegalArgumentException.class, () -> new Table(List.of("a", "a"), store));
        assertThrows(IllegalArgumentException.class, () -> new Table(List.of("a"), null));
        var table = new Table(List.of("a", "b"), store);
        assertThrows(IllegalArgumentException.class, () -> table.insert(1));
        assertThrows(IllegalArgumentException.class, () -> table.insert(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> table.createIndex("c"));
        table.createIndex("a");
        assertThrows(IllegalArgumentException.class, () -> table.createIndex("a"));
        assertThrows(IllegalArgumentException.class, () -> table.query(List.of(new Predicate.Eq("c", 1))));
        assertThrows(IllegalArgumentException.class, () -> table.query(null));
        assertEquals(0, table.size());
    }

    @Test
    void randomQueriesMatchAScan() {
        var random = new Random(121);
        for (int round = 0; round < 20; round++) {
            var store = new CountingStore();
            int rows = 300;
            var table = people(store, rows, round);
            if (random.nextBoolean()) {
                table.createIndex("city");
            }
            if (random.nextBoolean()) {
                table.createIndex("age");
            }
            var deleted = new ArrayList<Integer>();
            for (int i = 0; i < 30; i++) {
                int row = random.nextInt(rows);
                if (!deleted.contains(row)) {
                    assertTrue(table.delete(row));
                    deleted.add(row);
                }
            }
            for (int query = 0; query < 40; query++) {
                var where = new ArrayList<Predicate>();
                if (random.nextBoolean()) {
                    where.add(new Predicate.Eq("city", random.nextInt(10)));
                }
                if (random.nextBoolean()) {
                    long from = 18 + random.nextInt(60);
                    where.add(new Predicate.Range("age", from, from + random.nextInt(20)));
                }
                if (random.nextBoolean()) {
                    where.add(new Predicate.In("score", Set.of((long) random.nextInt(100_000), 5L)));
                }
                if (random.nextInt(4) == 0) {
                    where.add(new Predicate.In("city", Set.of(1L, 3L)));
                }
                assertEquals(scan(store, rows, deleted, where), table.query(where), "query " + where);
            }
        }
    }

    @Test
    void anIndexedQueryReadsOnlyTheRowsItsIndexGives() {
        var store = new CountingStore();
        var table = people(store, 100_000, 122);
        int score = (int) store.read(500)[3];
        table.createIndex("score");
        long matches = scan(store, 100_000, List.of(), List.of(new Predicate.Eq("score", score))).size();
        store.reads = 0;
        var found = table.query(List.of(new Predicate.Eq("score", score), new Predicate.Range("age", 18, 78)));
        assertTrue(found.contains(500));
        assertTrue(store.reads <= matches, store.reads + " rows read for an indexed predicate that matches " + matches);
        store.reads = 0;
        var band = table.query(List.of(new Predicate.Range("score", 1_000, 1_100), new Predicate.Eq("city", 3)));
        assertTrue(store.reads <= 120, store.reads + " rows read for a score band of about 100 rows");
        assertTrue(band.size() < 30);
        store.reads = 0;
        table.query(List.of(new Predicate.Eq("city", 3)));
        assertEquals(100_000, store.reads, "no index on city: a full scan reads every row");
    }

    @Test
    void indexedQueriesAreFast() {
        var store = new CountingStore();
        var table = people(store, 200_000, 123);
        table.createIndex("score");
        table.createIndex("age");
        var random = new Random(124);
        Structures.within(Duration.ofSeconds(6), () -> {
            long found = 0;
            for (int i = 0; i < 20_000; i++) {
                Structures.stopIfLate();
                long from = random.nextInt(100_000);
                found += table.query(List.of(new Predicate.Range("score", from, from + 20), new Predicate.Eq("city", 7))).size();
            }
            assertTrue(found > 0);
            for (int i = 0; i < 2_000; i++) {
                Structures.stopIfLate();
                table.delete(random.nextInt(200_000));
            }
        }, "20,000 indexed queries on 200,000 rows: is each one scanning the table?");
        var smallStore = new CountingStore();
        var small = people(smallStore, 200, 1);
        small.createIndex("age");
        Structures.assertNoJdkSortedStructures(small, smallStore);
    }
}
