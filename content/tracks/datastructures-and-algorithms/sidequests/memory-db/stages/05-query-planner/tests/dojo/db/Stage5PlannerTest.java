package dojo.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage5PlannerTest {

    /** The planner's people: a bitmap on city, ordered indexes on age and score. */
    private static Table indexed(CountingStore store, int rows, long seed) {
        var table = Stage3TableTest.people(store, rows, seed);
        table.createBitmapIndex("city");
        table.createIndex("age");
        table.createIndex("score");
        store.reads = 0;
        return table;
    }

    private static List<Integer> slowTop(CountingStore store, int rows, List<Integer> deleted, List<Predicate> where,
            String orderBy, int limit) {
        int c = Stage3TableTest.COLUMNS.indexOf(orderBy);
        var matches = new ArrayList<>(Stage3TableTest.scan(store, rows, deleted, where));
        var values = new long[rows];
        for (int row : matches) {
            values[row] = store.read(row)[c];
        }
        matches.sort(Comparator.<Integer>comparingLong(row -> values[row]).thenComparingInt(row -> row));
        return matches.subList(0, Math.min(limit, matches.size()));
    }

    @Test
    void explainPicksTheMostSelectiveIndex() {
        var store = new CountingStore();
        var table = indexed(store, 10_000, 141);
        var cityAndScore = List.<Predicate>of(new Predicate.Eq("city", 3), new Predicate.Range("score", 0, 500));
        long scoreRows = Stage3TableTest.scan(store, 10_000, List.of(), List.of(cityAndScore.get(1))).size();
        long cityRows = Stage3TableTest.scan(store, 10_000, List.of(), List.of(cityAndScore.get(0))).size();
        store.reads = 0;
        assertEquals(new Plan("score", scoreRows), table.explain(cityAndScore));
        assertEquals(new Plan("city", cityRows),
                table.explain(List.of(new Predicate.Eq("city", 3), new Predicate.Range("score", 0, 90_000))));
        assertEquals(new Plan(null, 10_000), table.explain(List.of(new Predicate.Eq("id", 7))), "no index: a full scan");
        assertEquals(new Plan(null, 10_000), table.explain(List.of()));
        assertEquals(new Plan("score", scoreRows),
                table.explain(List.of(new Predicate.Eq("id", 7), new Predicate.Range("score", 0, 500))));
        assertEquals(0, store.reads, "explain reads nothing");
    }

    @Test
    void tiesGoToTheFirstPredicate() {
        var table = new Table(List.of("a", "b"), new CountingStore());
        table.createIndex("a");
        table.createBitmapIndex("b");
        table.insert(1, 1);
        table.insert(2, 2);
        assertEquals(new Plan("b", 1), table.explain(List.of(new Predicate.Eq("b", 2), new Predicate.Eq("a", 1))));
        assertEquals(new Plan("a", 1), table.explain(List.of(new Predicate.Eq("a", 1), new Predicate.Eq("b", 2))));
        assertEquals(new Plan("a", 0), table.explain(List.of(new Predicate.Eq("b", 2), new Predicate.In("a", Set.of(7L)))));
        table.delete(1);
        assertEquals(new Plan("b", 0), table.explain(List.of(new Predicate.Eq("b", 2))), "deleted rows are not counted");
        assertThrows(IllegalArgumentException.class, () -> table.explain(List.of(new Predicate.Eq("z", 1))));
    }

    @Test
    void estimatesAreExact() {
        var random = new Random(142);
        var store = new CountingStore();
        var table = indexed(store, 3_000, 143);
        var deleted = new ArrayList<Integer>();
        for (int i = 0; i < 300; i++) {
            int row = random.nextInt(3_000);
            if (table.delete(row)) {
                deleted.add(row);
            }
        }
        for (int query = 0; query < 200; query++) {
            Predicate p = switch (random.nextInt(4)) {
                case 0 -> new Predicate.Eq("city", random.nextInt(10));
                case 1 -> new Predicate.In("city", Set.copyOf(List.of((long) random.nextInt(10), (long) random.nextInt(12))));
                case 2 -> {
                    long from = 18 + random.nextInt(60);
                    yield new Predicate.Range("age", from, from + random.nextInt(30));
                }
                default -> {
                    long from = random.nextInt(100_000);
                    yield new Predicate.Range("score", from, from + random.nextInt(50_000));
                }
            };
            long expected = Stage3TableTest.scan(store, 3_000, deleted, List.of(p)).size();
            assertEquals(new Plan(p.column(), expected), table.explain(List.of(p)), "explain " + p);
        }
    }

    @Test
    void topReturnsTheSmallestValuesInOrder() {
        var store = new CountingStore();
        var table = new Table(List.of("a", "b"), store);
        for (long[] row : new long[][] {{5, 1}, {3, 2}, {5, 2}, {1, 1}, {3, 1}}) {
            table.insert(row);
        }
        assertEquals(List.of(3, 1, 4), table.top(List.of(), "a", 3), "ties by row id");
        assertEquals(List.of(3, 4, 0), table.top(List.of(new Predicate.Eq("b", 1)), "a", 10));
        assertEquals(List.of(), table.top(List.of(), "a", 0));
        table.createIndex("a");
        table.createBitmapIndex("b");
        store.reads = 0;
        assertEquals(List.of(1, 2), table.top(List.of(new Predicate.Eq("b", 2)), "a", 5));
        assertEquals(0, store.reads, "an index on the order and on every predicate: nothing to read");
        assertThrows(IllegalArgumentException.class, () -> table.top(List.of(), "z", 1));
        assertThrows(IllegalArgumentException.class, () -> table.top(List.of(), "a", -1));
    }

    @Test
    void randomTopsMatchASort() {
        var random = new Random(144);
        var store = new CountingStore();
        var table = Stage3TableTest.people(store, 2_000, 145);
        table.createBitmapIndex("city");
        table.createIndex("age");
        var deleted = new ArrayList<Integer>();
        for (int i = 0; i < 200; i++) {
            int row = random.nextInt(2_000);
            if (table.delete(row)) {
                deleted.add(row);
            }
        }
        for (int query = 0; query < 100; query++) {
            var where = new ArrayList<Predicate>();
            if (random.nextBoolean()) {
                where.add(new Predicate.Eq("city", random.nextInt(10)));
            }
            if (random.nextBoolean()) {
                long from = random.nextInt(100_000);
                where.add(new Predicate.Range("score", from, from + 30_000));
            }
            String orderBy = List.of("age", "score", "id").get(random.nextInt(3));
            int limit = random.nextInt(30);
            assertEquals(slowTop(store, 2_000, deleted, where, orderBy, limit), table.top(where, orderBy, limit),
                    "top " + limit + " by " + orderBy + " where " + where);
        }
    }

    @Test
    void explainIsLogarithmic() {
        var store = new CountingStore();
        var table = indexed(store, 500_000, 146);
        var random = new Random(147);
        Structures.within(Duration.ofSeconds(6), () -> {
            long total = 0;
            for (int i = 0; i < 100_000; i++) {
                Structures.stopIfLate();
                long from = random.nextInt(50_000);
                var plan = table.explain(List.of(
                        new Predicate.Range("score", from, from + 50_000),
                        new Predicate.Range("age", 18, 48),
                        new Predicate.In("city", Set.of(1L, 2L, 3L, 4L, 5L))));
                total += plan.estimatedRows();
            }
            assertTrue(total > 0);
        }, "100,000 plans of ranges over half the table: are the rows walked or the bits counted?");
        assertEquals(0, store.reads);
    }

    @Test
    void topStopsEarlyOnAnIndex() {
        var store = new CountingStore();
        var table = indexed(store, 500_000, 148);
        var random = new Random(149);
        Structures.within(Duration.ofSeconds(6), () -> {
            for (int i = 0; i < 5_000; i++) {
                Structures.stopIfLate();
                var youngest = table.top(List.of(new Predicate.Eq("city", random.nextInt(10))), "age", 10);
                assertEquals(10, youngest.size());
            }
        }, "5,000 top-10 by an indexed column: walk the index and stop at the limit");
        assertEquals(0, store.reads);
        var unindexed = table.top(List.of(new Predicate.Eq("city", 2), new Predicate.Range("age", 20, 22)), "id", 3);
        assertEquals(3, unindexed.size());
        assertTrue(store.reads <= 20_000, store.reads + " reads: only the rows of city 2 aged 20 or 21 may be read");
    }
}
