package dojo;

import java.util.Set;

/** Classic dynamic programming problems. */
public final class Dp {
    private Dp() {}

    public static final long MOD = 1_000_000_007L;

    /** Right/down paths from the top-left to the bottom-right cell avoiding blocked cells, mod MOD. */
    public static long gridPaths(boolean[][] blocked) {
        throw new UnsupportedOperationException("todo gridPaths");
    }

    /** The Levenshtein distance between a and b. */
    public static int editDistance(String a, String b) {
        throw new UnsupportedOperationException("todo editDistance");
    }

    /** The length of the longest strictly increasing subsequence, in O(n log n). */
    public static int longestIncreasingSubsequence(int[] values) {
        throw new UnsupportedOperationException("todo longestIncreasingSubsequence");
    }

    /** The best total value of items taken at most once within the capacity. */
    public static long knapsack(int[] weights, int[] values, int capacity) {
        throw new UnsupportedOperationException("todo knapsack " + capacity);
    }

    /** The fewest coins summing to amount, or -1. */
    public static int minCoins(int[] coins, int amount) {
        throw new UnsupportedOperationException("todo minCoins " + amount);
    }

    /** Whether text splits into dictionary words. */
    public static boolean wordBreak(String text, Set<String> words) {
        throw new UnsupportedOperationException("todo wordBreak");
    }
}
