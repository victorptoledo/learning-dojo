package dojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Substring search: the KMP prefix function, overlapping matches, periods and rolling hashes. */
public final class TextSearch {
    private TextSearch() {}

    private static final long MOD = (1L << 61) - 1;
    private static final long BASE = 1_000_003L + new Random().nextInt(1 << 30); // random: no prepared collisions

    /** pi[i]: the length of the longest proper prefix of p[0..i] that is also its suffix. */
    public static int[] prefixFunction(String p) {
        if (p == null) throw new IllegalArgumentException("null string");
        int[] pi = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    /** Every start of the pattern in the text, overlapping matches included, in order. */
    public static List<Integer> findAll(String text, String pattern) {
        if (text == null) throw new IllegalArgumentException("null text");
        if (pattern == null || pattern.isEmpty()) throw new IllegalArgumentException("empty pattern");
        int[] pi = prefixFunction(pattern);
        int m = pattern.length();
        List<Integer> starts = new ArrayList<>();
        for (int i = 0, k = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (k > 0 && c != pattern.charAt(k)) k = pi[k - 1];
            if (c == pattern.charAt(k)) k++;
            if (k == m) {
                starts.add(i - m + 1);
                k = pi[k - 1];
            }
        }
        return starts;
    }

    /** The smallest p such that s is its first p characters repeated; 0 for the empty string. */
    public static int smallestPeriod(String s) {
        if (s == null) throw new IllegalArgumentException("null string");
        int n = s.length();
        if (n == 0) return 0;
        int p = n - prefixFunction(s)[n - 1];
        return n % p == 0 ? p : n;
    }

    /**
     * How many times (overlapping) each pattern occurs in the text. All patterns must have the
     * same, positive length; every pattern is a key of the result, with 0 if it never occurs.
     */
    public static Map<String, Integer> countAll(String text, Collection<String> patterns) {
        if (text == null || patterns == null) throw new IllegalArgumentException("null input");
        int m = -1;
        Map<Long, List<String>> byHash = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (String p : patterns) {
            if (p == null || p.isEmpty()) throw new IllegalArgumentException("empty pattern");
            if (m >= 0 && p.length() != m) throw new IllegalArgumentException("patterns of different lengths");
            m = p.length();
            if (counts.putIfAbsent(p, 0) == null) byHash.computeIfAbsent(hash(p, 0, m), h -> new ArrayList<>(1)).add(p);
        }
        if (m < 0 || m > text.length()) return counts;
        long high = power(m - 1); // BASE^(m-1), to remove the leaving character
        long h = hash(text, 0, m);
        for (int start = 0; ; start++) {
            List<String> candidates = byHash.get(h);
            if (candidates != null) {
                for (String p : candidates) {
                    if (text.regionMatches(start, p, 0, m)) counts.merge(p, 1, Integer::sum); // verify: hashes can collide
                }
            }
            if (start + m >= text.length()) break;
            h = add(mul(add(h, MOD - mul(text.charAt(start), high)), BASE), text.charAt(start + m));
        }
        return counts;
    }

    /**
     * The longest substring that occurs at least twice (occurrences may overlap); among the
     * longest, the one whose first occurrence starts earliest; "" if no character repeats.
     */
    public static String longestRepeated(String s) {
        if (s == null) throw new IllegalArgumentException("null string");
        int lo = 0, hi = s.length() - 1; // the answer is in [lo, hi]
        int bestStart = 0;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            int start = firstRepeated(s, mid);
            if (start >= 0) lo = mid;
            else hi = mid - 1;
        }
        if (lo == 0) return "";
        bestStart = firstRepeated(s, lo);
        return s.substring(bestStart, bestStart + lo);
    }

    /** The earliest first occurrence of a substring of this length that occurs twice, or -1. */
    private static int firstRepeated(String s, int length) {
        Map<Long, Integer> first = new HashMap<>();
        long high = power(length - 1);
        long h = hash(s, 0, length);
        int best = -1;
        for (int start = 0; start + length <= s.length(); start++) {
            if (start > 0) h = add(mul(add(h, MOD - mul(s.charAt(start - 1), high)), BASE), s.charAt(start + length - 1));
            Integer earlier = first.putIfAbsent(h, start);
            // verify only a repeat that would improve the answer: hashes can collide
            if (earlier != null && (best < 0 || earlier < best) && s.regionMatches(earlier, s, start, length)) best = earlier;
        }
        return best;
    }

    // ---- arithmetic modulo 2^61 - 1 ----

    private static long hash(String s, int from, int length) {
        long h = 0;
        for (int i = from; i < from + length; i++) h = add(mul(h, BASE), s.charAt(i));
        return h;
    }

    private static long power(int exponent) {
        long result = 1;
        for (int i = 0; i < exponent; i++) result = mul(result, BASE);
        return result;
    }

    private static long add(long a, long b) {
        long r = a + b;
        return r >= MOD ? r - MOD : r;
    }

    private static long mul(long a, long b) {
        long low = a * b;
        long high = Math.multiplyHigh(a, b);
        // a·b = high·2^64 + low (unsigned); 2^61 ≡ 1, so fold the bits above 61 back in
        long folded = (low & MOD) + ((low >>> 61) | (high << 3));
        return folded >= MOD ? folded - MOD : folded;
    }
}
