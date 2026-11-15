package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SearchGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(3);

    @Test
    void lower_bound_matches_a_scan() {
        var random = new Random(1);
        for (int round = 0; round < 2_000; round++) {
            int[] a = random.ints(random.nextInt(10), 0, 8).sorted().toArray();
            int target = random.nextInt(10) - 1;
            int expected = 0;
            while (expected < a.length && a[expected] < target) expected++;
            assertEquals(expected, Search.lowerBound(a, target), Arrays.toString(a) + " " + target);
        }
        assertEquals(0, Search.lowerBound(new int[0], 5));
    }

    @Test
    void lower_bound_is_logarithmic_and_does_not_overflow() {
        int[] big = new int[10_000_000];
        for (int i = 0; i < big.length; i++) big[i] = i;
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int i = 0; i < 200_000; i++) {
                assertEquals(big.length - 1 - i, Search.lowerBound(big, big.length - 1 - i));
            }
        });
    }

    @Test
    void search_rotated_finds_every_value_at_every_rotation() {
        for (int n = 1; n <= 12; n++) {
            for (int shift = 0; shift < n; shift++) {
                int[] a = new int[n];
                for (int i = 0; i < n; i++) a[i] = 10 * ((i + shift) % n);
                for (int i = 0; i < n; i++) {
                    assertEquals(i, Search.searchRotated(a, a[i]), Arrays.toString(a) + " " + a[i]);
                }
                assertEquals(-1, Search.searchRotated(a, 5), Arrays.toString(a));
                assertEquals(-1, Search.searchRotated(a, 1_000), Arrays.toString(a));
            }
        }
        assertEquals(-1, Search.searchRotated(new int[0], 1));
    }

    @Test
    void search_rotated_is_logarithmic() {
        int n = 5_000_000;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = (i + n / 3) % n;
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int v = 0; v < 200_000; v++) {
                int value = (v * 97) % n;
                assertEquals(value, a[Search.searchRotated(a, value)]);
            }
        });
    }

    private static int slowShip(int[] w, int days) {
        for (int c = 1; ; c++) {
            int used = 1;
            int load = 0;
            boolean fits = true;
            for (int x : w) {
                if (x > c) {
                    fits = false;
                    break;
                }
                if (load + x > c) {
                    used++;
                    load = 0;
                }
                load += x;
            }
            if (fits && used <= days) return c;
        }
    }

    @Test
    void ship_capacity_matches_trying_every_capacity() {
        var random = new Random(3);
        for (int round = 0; round < 300; round++) {
            int[] w = random.ints(1 + random.nextInt(8), 1, 10).toArray();
            int days = 1 + random.nextInt(w.length);
            assertEquals(slowShip(w, days), Search.minShipCapacity(w, days), Arrays.toString(w) + " " + days);
        }
        assertThrows(IllegalArgumentException.class, () -> Search.minShipCapacity(new int[] {1}, 0));
    }

    @Test
    void ship_capacity_searches_the_answer_not_every_capacity() {
        int[] w = new int[200_000];
        Arrays.fill(w, 10_000);
        assertTimeoutPreemptively(LIMIT, () -> {
            assertEquals(2_000_000_000 / 2, Search.minShipCapacity(w, 2), "half the packages per day");
            assertEquals(10_000, Search.minShipCapacity(w, 200_000));
        });
    }

    @Test
    void kth_smallest_matches_sorting_and_leaves_the_input_alone() {
        var random = new Random(4);
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(1 + random.nextInt(15), -5, 5).toArray();
            int[] before = a.clone();
            int[] sorted = a.clone();
            Arrays.sort(sorted);
            int k = 1 + random.nextInt(a.length);
            assertEquals(sorted[k - 1], Search.kthSmallest(a, k));
            assertArrayEquals(before, a, "the input must not change");
        }
        assertThrows(IllegalArgumentException.class, () -> Search.kthSmallest(new int[] {1, 2}, 0));
        assertThrows(IllegalArgumentException.class, () -> Search.kthSmallest(new int[] {1, 2}, 3));
    }

    @Test
    void kth_smallest_survives_sorted_and_repeated_inputs() {
        int n = 300_000;
        int[] sorted = new int[n];
        for (int i = 0; i < n; i++) sorted[i] = i;
        int[] same = new int[n];
        Arrays.fill(same, 7);
        assertTimeoutPreemptively(LIMIT, () -> {
            for (int k : new int[] {1, n / 2, n}) {
                assertEquals(k - 1, Search.kthSmallest(sorted, k));
                assertEquals(7, Search.kthSmallest(same, k));
            }
        });
    }

    @Test
    void inversions_match_counting_pairs() {
        var random = new Random(5);
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(random.nextInt(20), 0, 6).toArray();
            int[] before = a.clone();
            long expected = 0;
            for (int i = 0; i < a.length; i++) {
                for (int j = i + 1; j < a.length; j++) {
                    if (a[i] > a[j]) expected++;
                }
            }
            assertEquals(expected, Search.countInversions(a), Arrays.toString(a));
            assertArrayEquals(before, a, "the input must not change");
        }
    }

    @Test
    void inversions_are_counted_in_n_log_n() {
        int n = 300_000;
        int[] reversed = new int[n];
        for (int i = 0; i < n; i++) reversed[i] = n - i;
        long expected = (long) n * (n - 1) / 2;
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(expected, Search.countInversions(reversed)));
    }

    /** The number of multisets of candidates summing to target, by counting (not listing). */
    private static long ways(int[] candidates, int target) {
        long[] ways = new long[target + 1];
        ways[0] = 1;
        for (int c : candidates) {
            for (int t = c; t <= target; t++) ways[t] += ways[t - c];
        }
        return ways[target];
    }

    @Test
    void combinations_are_complete_valid_and_ordered() {
        int[][] cases = {{2, 3, 5}, {7, 3, 2}, {2}, {4, 9, 6}, {3, 5, 7, 11, 13}};
        int[] targets = {8, 18, 7, 30, 60};
        for (int c = 0; c < cases.length; c++) {
            int[] candidates = cases[c];
            int target = targets[c];
            int[] before = candidates.clone();
            List<List<Integer>> result = Search.combinationSum(candidates, target);
            assertArrayEquals(before, candidates, "the input must not change");
            assertEquals(ways(candidates, target), result.size(), "count for " + Arrays.toString(candidates) + " → " + target);
            assertEquals(result.size(), new HashSet<>(result).size(), "no duplicates");
            var allowed = new HashSet<Integer>();
            for (int x : candidates) allowed.add(x);
            for (int i = 0; i < result.size(); i++) {
                var combo = result.get(i);
                assertEquals(target, combo.stream().mapToInt(Integer::intValue).sum(), combo.toString());
                for (int j = 0; j < combo.size(); j++) {
                    assertTrue(allowed.contains(combo.get(j)), combo.toString());
                    if (j > 0) assertTrue(combo.get(j - 1) <= combo.get(j), "non-decreasing: " + combo);
                }
                if (i > 0) assertTrue(compare(result.get(i - 1), combo) < 0, "lexicographic order at " + i);
            }
        }
        assertEquals(List.of(), Search.combinationSum(new int[] {4, 6}, 7));
    }

    private static int compare(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }
}
