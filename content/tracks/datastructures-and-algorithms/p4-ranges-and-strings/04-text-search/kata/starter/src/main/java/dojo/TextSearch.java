package dojo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Substring search: the KMP prefix function, overlapping matches, periods and rolling hashes. */
public final class TextSearch {
    private TextSearch() {}

    /** pi[i]: the length of the longest proper prefix of p[0..i] that is also its suffix. */
    public static int[] prefixFunction(String p) {
        throw new UnsupportedOperationException("todo prefixFunction");
    }

    /** Every start of the pattern in the text, overlapping matches included, in order. */
    public static List<Integer> findAll(String text, String pattern) {
        throw new UnsupportedOperationException("todo findAll");
    }

    /** The smallest p such that s is its first p characters repeated; 0 for the empty string. */
    public static int smallestPeriod(String s) {
        throw new UnsupportedOperationException("todo smallestPeriod");
    }

    /** How many times (overlapping) each pattern, all of the same length, occurs in the text. */
    public static Map<String, Integer> countAll(String text, Collection<String> patterns) {
        throw new UnsupportedOperationException("todo countAll");
    }

    /** The longest substring that occurs at least twice; among those, the one that occurs first. */
    public static String longestRepeated(String s) {
        throw new UnsupportedOperationException("todo longestRepeated");
    }
}
