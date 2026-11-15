package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SequencesGradingTest {
    private static final int BIG = 200_000;
    private static final Duration LIMIT = Duration.ofSeconds(3);

    // ---- slow but obviously correct answers to compare with ----

    private static int[] slowPair(int[] a, int target) {
        for (int i = 0; i < a.length; i++) {
            for (int j = a.length - 1; j > i; j--) {
                if ((long) a[i] + a[j] == target) return new int[] {i, j};
            }
        }
        return new int[0];
    }

    private static int slowUnique(String s) {
        int best = 0;
        for (int i = 0; i < s.length(); i++) {
            var seen = new HashSet<Character>();
            int j = i;
            while (j < s.length() && seen.add(s.charAt(j))) j++;
            best = Math.max(best, j - i);
        }
        return best;
    }

    private static int[] slowNextGreater(int[] a) {
        int[] out = new int[a.length];
        Arrays.fill(out, -1);
        for (int i = 0; i < a.length; i++) {
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] > a[i]) {
                    out[i] = a[j];
                    break;
                }
            }
        }
        return out;
    }

    private static long slowCount(int[] a, long k) {
        long count = 0;
        for (int i = 0; i < a.length; i++) {
            long sum = 0;
            for (int j = i; j < a.length; j++) {
                sum += a[j];
                if (sum == k) count++;
            }
        }
        return count;
    }

    private static int slowWindow(int[] a, long atLeast) {
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            long sum = 0;
            for (int j = i; j < a.length; j++) {
                sum += a[j];
                if (sum >= atLeast) {
                    if (best == 0 || j - i + 1 < best) best = j - i + 1;
                    break;
                }
            }
        }
        return best;
    }

    // ---- small inputs against the slow answers ----

    @Test
    void pair_with_sum_matches_on_small_random_inputs() {
        var random = new Random(1);
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(random.nextInt(12), -10, 10).sorted().toArray();
            int target = random.nextInt(30) - 15;
            assertArrayEquals(slowPair(a, target), Sequences.pairWithSum(a, target), Arrays.toString(a) + " " + target);
        }
        assertArrayEquals(new int[0], Sequences.pairWithSum(new int[0], 3));
        assertArrayEquals(new int[0], Sequences.pairWithSum(new int[] {4}, 8));
        int max = Integer.MAX_VALUE;
        assertArrayEquals(new int[0], Sequences.pairWithSum(new int[] {max, max}, -2), "no int overflow");
    }

    @Test
    void longest_unique_run_matches_on_small_random_inputs() {
        var random = new Random(2);
        assertEquals(0, Sequences.longestUniqueRun(""));
        assertEquals(1, Sequences.longestUniqueRun("aaaa"));
        assertEquals(3, Sequences.longestUniqueRun("pwwkew"));
        assertEquals(2, Sequences.longestUniqueRun("abba"));
        for (int round = 0; round < 500; round++) {
            var sb = new StringBuilder();
            int length = random.nextInt(15);
            for (int i = 0; i < length; i++) sb.append((char) ('a' + random.nextInt(5)));
            String s = sb.toString();
            assertEquals(slowUnique(s), Sequences.longestUniqueRun(s), s);
        }
    }

    @Test
    void next_greater_matches_on_small_random_inputs() {
        var random = new Random(3);
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(random.nextInt(12), 0, 6).toArray();
            assertArrayEquals(slowNextGreater(a), Sequences.nextGreater(a), Arrays.toString(a));
        }
    }

    @Test
    void balanced_brackets() {
        assertTrue(Sequences.balanced(""));
        assertTrue(Sequences.balanced("no brackets at all"));
        assertTrue(Sequences.balanced("{[()()]}([])"));
        assertFalse(Sequences.balanced("(("));
        assertFalse(Sequences.balanced("))"));
        assertFalse(Sequences.balanced("(]"));
        assertFalse(Sequences.balanced("([)]"));
        assertFalse(Sequences.balanced("}{"));
        assertFalse(Sequences.balanced("a(b)c)"));
    }

    @Test
    void subarray_sums_match_on_small_random_inputs() {
        var random = new Random(4);
        assertEquals(4, Sequences.countSubarraysWithSum(new int[] {3, -3, 3, -3}, 0));
        assertEquals(0, Sequences.countSubarraysWithSum(new int[0], 0));
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(random.nextInt(12), -3, 4).toArray();
            long k = random.nextInt(7) - 3;
            assertEquals(slowCount(a, k), Sequences.countSubarraysWithSum(a, k), Arrays.toString(a) + " " + k);
        }
    }

    @Test
    void shortest_window_matches_on_small_random_inputs() {
        var random = new Random(5);
        assertEquals(0, Sequences.shortestWindowWithSum(new int[] {1, 1, 1}, 10));
        assertEquals(1, Sequences.shortestWindowWithSum(new int[] {1, 9, 1}, 9));
        for (int round = 0; round < 500; round++) {
            int[] a = random.ints(random.nextInt(12), 1, 8).toArray();
            long atLeast = random.nextInt(30) + 1;
            assertEquals(slowWindow(a, atLeast), Sequences.shortestWindowWithSum(a, atLeast), Arrays.toString(a) + " " + atLeast);
        }
    }

    // ---- large inputs: only linear answers finish in time ----

    @Test
    void large_pair_with_sum_is_linear() {
        int[] a = new int[BIG];
        for (int i = 0; i < BIG; i++) a[i] = 2 * i;
        // every sum is even, so an odd target has no pair
        assertTimeoutPreemptively(LIMIT, () -> assertArrayEquals(new int[0], Sequences.pairWithSum(a, 1)));
        assertTimeoutPreemptively(LIMIT, () -> assertArrayEquals(new int[] {BIG - 2, BIG - 1}, Sequences.pairWithSum(a, 4 * BIG - 6)));
    }

    @Test
    void large_unique_run_is_linear() {
        var sb = new StringBuilder();
        for (int i = 0; i < BIG; i++) sb.append((char) ('A' + i % 50));
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(50, Sequences.longestUniqueRun(sb.toString())));
        var distinct = new StringBuilder();
        for (int i = 0; i < 50_000; i++) distinct.append((char) (0x0100 + i));
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(50_000, Sequences.longestUniqueRun(distinct.toString())));
    }

    @Test
    void large_next_greater_is_linear() {
        int[] decreasing = new int[BIG];
        for (int i = 0; i < BIG; i++) decreasing[i] = BIG - i;
        decreasing[BIG - 1] = BIG + 1;
        int[] answer = assertTimeoutPreemptively(LIMIT, () -> Sequences.nextGreater(decreasing));
        assertEquals(BIG + 1, answer[0]);
        assertEquals(-1, answer[BIG - 1]);
    }

    @Test
    void large_balanced_is_linear() {
        var sb = new StringBuilder();
        for (int i = 0; i < BIG; i++) sb.append("([{");
        for (int i = 0; i < BIG; i++) sb.append("}])");
        assertTimeoutPreemptively(LIMIT, () -> assertTrue(Sequences.balanced(sb.toString())));
    }

    @Test
    void large_subarray_count_is_linear() {
        int[] zeros = new int[BIG];
        long expected = (long) BIG * (BIG + 1) / 2;
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(expected, Sequences.countSubarraysWithSum(zeros, 0)));
    }

    @Test
    void large_shortest_window_is_linear() {
        int[] ones = new int[BIG];
        Arrays.fill(ones, 1);
        ones[BIG - 1] = 1_000;
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(1, Sequences.shortestWindowWithSum(ones, 1_000)));
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(BIG / 2, Sequences.shortestWindowWithSum(ones, BIG / 2 + 999)));
    }
}
