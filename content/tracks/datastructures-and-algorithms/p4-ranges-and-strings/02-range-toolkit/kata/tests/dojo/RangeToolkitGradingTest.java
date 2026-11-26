package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RangeToolkitGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(5);

    private static long[] randomValues(Random random, int n, int bound, boolean negatives) {
        long[] v = new long[n];
        for (int i = 0; i < n; i++) v[i] = negatives ? random.nextInt(2 * bound + 1) - bound : random.nextInt(bound + 1);
        return v;
    }

    // ---- correctness against loops ----

    @Test
    void range_adds_match_a_loop() {
        Random random = new Random(1);
        for (int round = 0; round < 200; round++) {
            int n = random.nextInt(12);
            int[][] updates = new int[random.nextInt(8)][];
            long[] expected = new long[n];
            for (int u = 0; u < updates.length; u++) {
                int lo = random.nextInt(n + 1);
                int hi = lo + random.nextInt(n - lo + 1);
                int delta = random.nextInt(21) - 10;
                updates[u] = new int[] {lo, hi, delta};
                for (int i = lo; i < hi; i++) expected[i] += delta;
            }
            assertArrayEquals(expected, RangeToolkit.applyRangeAdds(n, updates));
        }
        assertArrayEquals(new long[0], RangeToolkit.applyRangeAdds(0, new int[][] {{0, 0, 7}}));
        assertArrayEquals(new long[] {3_000_000_000L}, RangeToolkit.applyRangeAdds(1, new int[][] {{0, 1, 2_000_000_000}, {0, 1, 1_000_000_000}}));
    }

    @Test
    void fenwick_matches_a_plain_array() {
        Random random = new Random(2);
        for (int round = 0; round < 200; round++) {
            int n = random.nextInt(20);
            long[] plain = randomValues(random, n, 9, true);
            var f = new RangeToolkit.Fenwick(plain);
            plain = plain.clone();
            assertEquals(n, f.size());
            for (int step = 0; step < 40; step++) {
                if (n > 0 && random.nextBoolean()) {
                    int i = random.nextInt(n);
                    long v = random.nextInt(19) - 9;
                    if (random.nextBoolean()) {
                        f.set(i, v);
                        plain[i] = v;
                    } else {
                        f.add(i, v);
                        plain[i] += v;
                    }
                }
                int lo = random.nextInt(n + 1);
                int hi = lo + random.nextInt(n - lo + 1);
                long expected = 0;
                for (int i = lo; i < hi; i++) expected += plain[i];
                assertEquals(expected, f.sum(lo, hi), "sum(" + lo + ", " + hi + ")");
                if (n > 0) {
                    int i = random.nextInt(n);
                    assertEquals(plain[i], f.get(i));
                }
            }
        }
    }

    @Test
    void the_constructor_copies_its_array() {
        long[] values = {1, 2, 3};
        var f = new RangeToolkit.Fenwick(values);
        var s = new RangeToolkit.SparseMin(values);
        var t = new RangeToolkit.MaxTree(values);
        values[0] = 100;
        values[1] = -100;
        assertEquals(6, f.sum(0, 3));
        assertEquals(1, f.get(0));
        assertEquals(1, f.firstReaching(2));
        assertEquals(1, s.min(0, 3));
        assertEquals(3, t.max(0, 3));
    }

    @Test
    void search_by_prefix_matches_a_scan() {
        Random random = new Random(3);
        for (int round = 0; round < 300; round++) {
            int n = 1 + random.nextInt(40);
            long[] plain = randomValues(random, n, 5, false);
            var f = new RangeToolkit.Fenwick(plain);
            for (int step = 0; step < 10; step++) {
                int i = random.nextInt(n);
                long v = random.nextInt(6);
                f.set(i, v);
                plain[i] = v;
                long total = 0;
                for (long x : plain) total += x;
                long target = 1 + random.nextInt((int) total + 3);
                int expected = -1;
                long running = 0;
                for (int p = 0; p < n; p++) {
                    running += plain[p];
                    if (running >= target) {
                        expected = p;
                        break;
                    }
                }
                assertEquals(expected, f.firstReaching(target), "firstReaching(" + target + ")");
            }
        }
        assertEquals(-1, new RangeToolkit.Fenwick(new long[0]).firstReaching(1));
        // zeros before the answer are skipped, and sizes that are not powers of two work
        assertEquals(6, new RangeToolkit.Fenwick(new long[] {0, 0, 0, 0, 0, 0, 1}).firstReaching(1));
    }

    @Test
    void search_by_prefix_needs_a_positive_target_and_no_negative_values() {
        var f = new RangeToolkit.Fenwick(new long[] {2, -1, 4});
        assertThrows(IllegalStateException.class, () -> f.firstReaching(1));
        f.set(1, 0);
        assertEquals(2, f.firstReaching(3));
        assertThrows(IllegalArgumentException.class, () -> f.firstReaching(0));
        f.add(2, -10);
        assertThrows(IllegalStateException.class, () -> f.firstReaching(1));
        f.add(2, 10);
        assertEquals(0, f.firstReaching(1));
    }

    @Test
    void sparse_min_and_max_tree_match_loops() {
        Random random = new Random(4);
        for (int round = 0; round < 200; round++) {
            int n = 1 + random.nextInt(30);
            long[] plain = randomValues(random, n, 6, true); // small values: many ties
            var sparse = new RangeToolkit.SparseMin(plain);
            for (int q = 0; q < 30; q++) {
                int lo = random.nextInt(n);
                int hi = lo + 1 + random.nextInt(n - lo);
                long min = Long.MAX_VALUE;
                for (int i = lo; i < hi; i++) min = Math.min(min, plain[i]);
                assertEquals(min, sparse.min(lo, hi), "min(" + lo + ", " + hi + ")");
            }
            var tree = new RangeToolkit.MaxTree(plain);
            plain = plain.clone();
            for (int q = 0; q < 30; q++) {
                if (random.nextBoolean()) {
                    int i = random.nextInt(n);
                    long v = random.nextInt(13) - 6;
                    tree.set(i, v);
                    plain[i] = v;
                }
                int lo = random.nextInt(n);
                int hi = lo + 1 + random.nextInt(n - lo);
                int arg = lo;
                for (int i = lo; i < hi; i++) if (plain[i] > plain[arg]) arg = i;
                assertEquals(arg, tree.argMax(lo, hi), "argMax(" + lo + ", " + hi + ")");
                assertEquals(plain[arg], tree.max(lo, hi));
            }
        }
    }

    @Test
    void bad_ranges_and_indexes_are_refused() {
        long[] values = {4, 5, 6};
        var f = new RangeToolkit.Fenwick(values);
        var s = new RangeToolkit.SparseMin(values);
        var t = new RangeToolkit.MaxTree(values);
        for (int[] r : new int[][] {{-1, 2}, {0, 4}, {2, 1}}) {
            assertThrows(IndexOutOfBoundsException.class, () -> f.sum(r[0], r[1]));
            assertThrows(IndexOutOfBoundsException.class, () -> s.min(r[0], r[1]));
            assertThrows(IndexOutOfBoundsException.class, () -> t.argMax(r[0], r[1]));
            assertThrows(IndexOutOfBoundsException.class, () -> RangeToolkit.applyRangeAdds(3, new int[][] {{r[0], r[1], 1}}));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> f.add(3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> f.set(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> f.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> t.set(3, 1));
        assertThrows(IllegalArgumentException.class, () -> s.min(1, 1));
        assertThrows(IllegalArgumentException.class, () -> t.max(2, 2));
        assertThrows(IllegalArgumentException.class, () -> RangeToolkit.applyRangeAdds(-1, new int[0][]));
        assertEquals(0, f.sum(1, 1));
        assertEquals(15, f.sum(0, 3)); // nothing above changed the values
    }

    // ---- scale ----

    @Test
    void many_range_adds() {
        assertTimeoutPreemptively(LIMIT, () -> {
            int n = 300_000;
            Random random = new Random(5);
            int[][] updates = new int[300_000][];
            for (int u = 0; u < updates.length; u++) {
                int lo = random.nextInt(n / 10);
                updates[u] = new int[] {lo, n - random.nextInt(n / 10), 1};
            }
            long[] values = RangeToolkit.applyRangeAdds(n, updates);
            assertEquals(300_000, values[n / 2]);
        }, "range adds must use a difference array, not a loop per update");
    }

    @Test
    void fenwick_with_a_million_values() {
        assertTimeoutPreemptively(LIMIT, () -> {
            int n = 1_000_000;
            Random random = new Random(6);
            var f = new RangeToolkit.Fenwick(randomValues(random, n, 100, false));
            long check = 0;
            for (int q = 0; q < 400_000; q++) {
                f.add(random.nextInt(n), random.nextInt(10));
                int lo = random.nextInt(n / 2);
                check += f.sum(lo, lo + n / 2);
                check += f.firstReaching(1 + random.nextInt(45_000_000)); // the total is about 50 million
            }
            assertTrue(check > 0);
        }, "sum and firstReaching must be O(log n) or close, not a scan");
    }

    @Test
    void sparse_min_with_a_million_values() {
        assertTimeoutPreemptively(LIMIT, () -> {
            int n = 1_000_000;
            Random random = new Random(7);
            var s = new RangeToolkit.SparseMin(randomValues(random, n, 1_000_000, true));
            long check = 0;
            for (int q = 0; q < 2_000_000; q++) {
                int lo = random.nextInt(n / 2);
                check += s.min(lo, lo + 1 + random.nextInt(n / 2));
            }
            assertTrue(check < 0);
        }, "min must look up two precomputed ranges, not scan");
    }

    @Test
    void max_tree_with_half_a_million_values() {
        assertTimeoutPreemptively(LIMIT, () -> {
            int n = 500_000;
            Random random = new Random(8);
            var t = new RangeToolkit.MaxTree(randomValues(random, n, 1_000_000, true));
            long check = 0;
            for (int q = 0; q < 500_000; q++) {
                t.set(random.nextInt(n), random.nextInt(2_000_000) - 1_000_000);
                int lo = random.nextInt(n / 2);
                check += t.argMax(lo, lo + 1 + random.nextInt(n / 2));
            }
            assertTrue(check > 0);
        }, "set and argMax must be O(log n): a segment tree");
    }
}
