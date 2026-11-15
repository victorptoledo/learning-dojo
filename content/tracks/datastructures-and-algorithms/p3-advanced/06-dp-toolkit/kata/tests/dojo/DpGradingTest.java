package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DpGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(4);

    // ---- brute force references for small inputs ----

    private static long slowPaths(boolean[][] b, int r, int c) {
        if (r >= b.length || c >= b[0].length || b[r][c]) return 0;
        if (r == b.length - 1 && c == b[0].length - 1) return 1;
        return slowPaths(b, r + 1, c) + slowPaths(b, r, c + 1);
    }

    private static int slowEdit(String a, String b) {
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();
        if (a.charAt(0) == b.charAt(0)) return slowEdit(a.substring(1), b.substring(1));
        return 1 + Math.min(slowEdit(a.substring(1), b.substring(1)), Math.min(slowEdit(a.substring(1), b), slowEdit(a, b.substring(1))));
    }

    private static int slowLis(int[] v) {
        int best = 0;
        for (int mask = 0; mask < (1 << v.length); mask++) {
            int last = Integer.MIN_VALUE;
            int length = 0;
            boolean ok = true;
            for (int i = 0; i < v.length && ok; i++) {
                if ((mask & (1 << i)) != 0) {
                    if (length > 0 && v[i] <= last) ok = false;
                    last = v[i];
                    length++;
                }
            }
            if (ok) best = Math.max(best, length);
        }
        return best;
    }

    private static long slowKnapsack(int[] w, int[] v, int capacity) {
        long best = 0;
        for (int mask = 0; mask < (1 << w.length); mask++) {
            long weight = 0;
            long value = 0;
            for (int i = 0; i < w.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    weight += w[i];
                    value += v[i];
                }
            }
            if (weight <= capacity) best = Math.max(best, value);
        }
        return best;
    }

    private static int slowCoins(int[] coins, int amount) {
        if (amount == 0) return 0;
        int best = -1;
        for (int c : coins) {
            if (c <= amount) {
                int rest = slowCoins(coins, amount - c);
                if (rest >= 0 && (best < 0 || rest + 1 < best)) best = rest + 1;
            }
        }
        return best;
    }

    private static boolean slowBreak(String text, Set<String> words) {
        if (text.isEmpty()) return true;
        for (int i = 1; i <= text.length(); i++) {
            if (words.contains(text.substring(0, i)) && slowBreak(text.substring(i), words)) return true;
        }
        return false;
    }

    private static String randomText(Random random, int maxLength) {
        var sb = new StringBuilder();
        int length = random.nextInt(maxLength + 1);
        for (int i = 0; i < length; i++) sb.append((char) ('a' + random.nextInt(3)));
        return sb.toString();
    }

    // ---- small inputs ----

    @Test
    void grid_paths_match_brute_force() {
        var random = new Random(61);
        for (int round = 0; round < 300; round++) {
            var b = new boolean[1 + random.nextInt(6)][1 + random.nextInt(6)];
            for (boolean[] row : b) for (int c = 0; c < row.length; c++) row[c] = random.nextInt(4) == 0;
            assertEquals(slowPaths(b, 0, 0), Dp.gridPaths(b));
        }
        assertEquals(1, Dp.gridPaths(new boolean[1][1]));
        assertEquals(0, Dp.gridPaths(new boolean[][] {{true}}));
    }

    @Test
    void edit_distance_matches_brute_force() {
        var random = new Random(62);
        for (int round = 0; round < 300; round++) {
            String a = randomText(random, 6);
            String b = randomText(random, 6);
            assertEquals(slowEdit(a, b), Dp.editDistance(a, b), a + " → " + b);
        }
        assertEquals(0, Dp.editDistance("", ""));
    }

    @Test
    void lis_matches_brute_force() {
        var random = new Random(63);
        for (int round = 0; round < 300; round++) {
            int[] v = random.ints(random.nextInt(12), 0, 6).toArray();
            assertEquals(slowLis(v), Dp.longestIncreasingSubsequence(v), Arrays.toString(v));
        }
        assertEquals(1, Dp.longestIncreasingSubsequence(new int[] {5, 5, 5}), "strictly increasing");
    }

    @Test
    void knapsack_matches_brute_force() {
        var random = new Random(64);
        for (int round = 0; round < 300; round++) {
            int n = random.nextInt(10);
            int[] w = random.ints(n, 0, 10).toArray();
            int[] v = random.ints(n, 0, 20).toArray();
            int capacity = random.nextInt(30);
            assertEquals(slowKnapsack(w, v, capacity), Dp.knapsack(w, v, capacity));
        }
    }

    @Test
    void min_coins_matches_brute_force_and_beats_greedy() {
        var random = new Random(65);
        for (int round = 0; round < 300; round++) {
            int[] coins = random.ints(1 + random.nextInt(3), 2, 9).distinct().toArray();
            int amount = random.nextInt(25);
            assertEquals(slowCoins(coins, amount), Dp.minCoins(coins, amount), Arrays.toString(coins) + " " + amount);
        }
        assertEquals(2, Dp.minCoins(new int[] {1, 3, 4}, 6));
        assertEquals(-1, Dp.minCoins(new int[] {2}, 3));
        assertEquals(0, Dp.minCoins(new int[] {7}, 0));
    }

    @Test
    void word_break_matches_brute_force() {
        var random = new Random(66);
        for (int round = 0; round < 300; round++) {
            var words = new HashSet<String>();
            for (int i = 0; i < 4; i++) {
                String w = randomText(random, 3);
                if (!w.isEmpty()) words.add(w);
            }
            String text = randomText(random, 10);
            assertEquals(slowBreak(text, words), Dp.wordBreak(text, words), text + " " + words);
        }
        assertTrue(Dp.wordBreak("", Set.of("a")));
    }

    // ---- large inputs ----

    @Test
    void a_large_open_grid_needs_a_table() {
        var open = new boolean[1_500][1_500];
        long paths = assertTimeoutPreemptively(LIMIT, () -> Dp.gridPaths(open));
        assertTrue(paths >= 0 && paths < Dp.MOD);
        var wall = new boolean[1_500][1_500];
        for (int c = 0; c < 1_500; c++) wall[700][c] = true;
        assertEquals(0, assertTimeoutPreemptively(LIMIT, () -> Dp.gridPaths(wall)));
        var corridor = new boolean[2][2_000];
        assertEquals(2_000, assertTimeoutPreemptively(LIMIT, () -> Dp.gridPaths(corridor)));
    }

    @Test
    void long_strings_have_an_edit_distance() {
        var random = new Random(67);
        var sa = new StringBuilder();
        var sb = new StringBuilder();
        for (int i = 0; i < 3_000; i++) {
            char c = (char) ('a' + random.nextInt(4));
            sa.append(c);
            sb.append(i % 100 == 0 ? 'z' : c); // 30 substitutions
        }
        assertEquals(30, assertTimeoutPreemptively(LIMIT, () -> Dp.editDistance(sa.toString(), sb.toString())));
        assertEquals(3_000, assertTimeoutPreemptively(LIMIT, () -> Dp.editDistance(sa.toString(), "")));
    }

    @Test
    void lis_of_two_hundred_thousand_values_is_n_log_n() {
        int n = 200_000;
        int[] increasing = new int[n];
        for (int i = 0; i < n; i++) increasing[i] = i;
        assertEquals(n, assertTimeoutPreemptively(LIMIT, () -> Dp.longestIncreasingSubsequence(increasing)));
        int[] zigzag = new int[n];
        for (int i = 0; i < n; i++) zigzag[i] = i % 2 == 0 ? i : i - 2;
        int lis = assertTimeoutPreemptively(LIMIT, () -> Dp.longestIncreasingSubsequence(zigzag));
        assertTrue(lis >= n / 2 && lis <= n / 2 + 1, "lis " + lis);
    }

    @Test
    void knapsack_and_coins_with_large_capacities() {
        var random = new Random(68);
        int[] w = random.ints(200, 1, 1_000).toArray();
        int[] v = random.ints(200, 1, 1_000).toArray();
        long best = assertTimeoutPreemptively(LIMIT, () -> Dp.knapsack(w, v, 50_000));
        long all = Arrays.stream(v).asLongStream().sum();
        assertTrue(best > 0 && best <= all);
        assertEquals(all, Dp.knapsack(w, v, Arrays.stream(w).sum()), "everything fits");
        int[] coins = {7, 11, 13, 101, 997, 2003, 5003, 10_007};
        int fewest = assertTimeoutPreemptively(LIMIT, () -> Dp.minCoins(coins, 1_000_000));
        assertTrue(fewest > 0 && fewest <= 1_000_000 / 7);
    }

    @Test
    void word_break_with_many_overlapping_splits() {
        var words = new HashSet<String>();
        for (int i = 1; i <= 20; i++) words.add("a".repeat(i));
        String impossible = "a".repeat(3_000) + "b";
        assertFalse(assertTimeoutPreemptively(LIMIT, () -> Dp.wordBreak(impossible, words)));
        assertTrue(assertTimeoutPreemptively(LIMIT, () -> Dp.wordBreak("a".repeat(3_000), words)));
    }
}
