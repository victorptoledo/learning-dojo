package dojo;

import java.util.Arrays;
import java.util.Set;

/** Classic dynamic programming problems. */
public final class Dp {
    private Dp() {}

    public static final long MOD = 1_000_000_007L;

    public static long gridPaths(boolean[][] blocked) {
        int rows = blocked.length;
        if (rows == 0) return 0;
        int cols = blocked[0].length;
        long[] paths = new long[cols]; // one row at a time
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (blocked[r][c]) {
                    paths[c] = 0;
                } else if (r == 0 && c == 0) {
                    paths[c] = 1;
                } else if (c > 0) {
                    paths[c] = (paths[c] + paths[c - 1]) % MOD; // from above + from the left
                }
            }
        }
        return paths[cols - 1];
    }

    public static int editDistance(String a, String b) {
        if (a.length() < b.length()) return editDistance(b, a);
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitute = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitute, Math.min(previous[j], current[j - 1]) + 1);
            }
            int[] t = previous;
            previous = current;
            current = t;
        }
        return previous[b.length()];
    }

    public static int longestIncreasingSubsequence(int[] values) {
        int[] tails = new int[values.length];
        int length = 0;
        for (int v : values) {
            int lo = 0;
            int hi = length;
            while (lo < hi) { // first tail >= v
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < v) {
                    lo = mid + 1;
                } else {
                    hi = mid;
                }
            }
            tails[lo] = v;
            if (lo == length) length++;
        }
        return length;
    }

    public static long knapsack(int[] weights, int[] values, int capacity) {
        long[] best = new long[capacity + 1];
        for (int item = 0; item < weights.length; item++) {
            for (int c = capacity; c >= weights[item]; c--) { // high to low: each item once
                best[c] = Math.max(best[c], best[c - weights[item]] + values[item]);
            }
        }
        return best[capacity];
    }

    public static int minCoins(int[] coins, int amount) {
        int unreachable = Integer.MAX_VALUE;
        int[] fewest = new int[amount + 1];
        Arrays.fill(fewest, unreachable);
        fewest[0] = 0;
        for (int a = 1; a <= amount; a++) {
            for (int coin : coins) {
                if (coin <= a && fewest[a - coin] != unreachable) {
                    fewest[a] = Math.min(fewest[a], fewest[a - coin] + 1);
                }
            }
        }
        return fewest[amount] == unreachable ? -1 : fewest[amount];
    }

    public static boolean wordBreak(String text, Set<String> words) {
        int longest = 0;
        for (String w : words) longest = Math.max(longest, w.length());
        boolean[] can = new boolean[text.length() + 1];
        can[0] = true;
        for (int end = 1; end <= text.length(); end++) {
            for (int start = Math.max(0, end - longest); start < end && !can[end]; start++) {
                can[end] = can[start] && words.contains(text.substring(start, end));
            }
        }
        return can[text.length()];
    }
}
