package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TextSearchGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(5);

    private static String random(Random random, int length, int alphabet) {
        char[] c = new char[length];
        for (int i = 0; i < length; i++) c[i] = (char) ('a' + random.nextInt(alphabet));
        return new String(c);
    }

    // ---- slow references ----

    private static int[] slowPi(String p) {
        int[] pi = new int[p.length()];
        for (int i = 0; i < p.length(); i++) {
            for (int k = i; k > 0; k--) {
                if (p.substring(0, k).equals(p.substring(i - k + 1, i + 1))) {
                    pi[i] = k;
                    break;
                }
            }
        }
        return pi;
    }

    private static List<Integer> slowFind(String t, String p) {
        List<Integer> starts = new ArrayList<>();
        for (int i = 0; i + p.length() <= t.length(); i++) if (t.startsWith(p, i)) starts.add(i);
        return starts;
    }

    private static int slowPeriod(String s) {
        for (int p = 1; p <= s.length(); p++) {
            if (s.length() % p == 0 && s.substring(0, p).repeat(s.length() / p).equals(s)) return p;
        }
        return 0;
    }

    private static String slowRepeated(String s) {
        for (int length = s.length() - 1; length > 0; length--) {
            for (int i = 0; i + length <= s.length(); i++) {
                if (s.indexOf(s.substring(i, i + length), i + 1) >= 0) return s.substring(i, i + length);
            }
        }
        return "";
    }

    @Test
    void every_function_matches_a_slow_reference() {
        Random random = new Random(1);
        for (int round = 0; round < 600; round++) {
            int alphabet = 1 + random.nextInt(3);
            String text = random(random, random.nextInt(30), alphabet);
            String pattern = random(random, 1 + random.nextInt(5), alphabet);
            assertArrayEquals(slowPi(pattern), TextSearch.prefixFunction(pattern), "prefixFunction " + pattern);
            assertEquals(slowFind(text, pattern), TextSearch.findAll(text, pattern), "findAll in " + text + " of " + pattern);
            assertEquals(slowPeriod(text), TextSearch.smallestPeriod(text), "smallestPeriod " + text);
            assertEquals(slowRepeated(text), TextSearch.longestRepeated(text), "longestRepeated " + text);
            List<String> patterns = new ArrayList<>();
            int m = 1 + random.nextInt(4);
            for (int i = 0; i < 1 + random.nextInt(5); i++) patterns.add(random(random, m, alphabet));
            Map<String, Integer> expected = new HashMap<>();
            for (String p : patterns) expected.put(p, slowFind(text, p).size());
            assertEquals(expected, TextSearch.countAll(text, patterns), "countAll in " + text + " of " + patterns);
        }
    }

    @Test
    void periods_and_edges() {
        assertEquals(0, TextSearch.smallestPeriod(""));
        assertEquals(1, TextSearch.smallestPeriod("q"));
        assertEquals(5, TextSearch.smallestPeriod("abaab"), "a border that does not divide the length is not a period");
        assertEquals(3, TextSearch.smallestPeriod("abaabaaba"));
        assertArrayEquals(new int[0], TextSearch.prefixFunction(""));
        assertEquals(List.of(), TextSearch.findAll("", "a"));
        assertEquals(List.of(), TextSearch.findAll("ab", "abc"));
        assertEquals("", TextSearch.longestRepeated(""));
        assertEquals("", TextSearch.longestRepeated("x"));
        assertEquals("aaa", TextSearch.longestRepeated("aaaa"), "occurrences may overlap");
        assertEquals("ab", TextSearch.longestRepeated("abxyab"));
        assertEquals("ab", TextSearch.longestRepeated("abcdcdab"), "among the longest, the one that occurs first");
        assertEquals("😀", TextSearch.longestRepeated("😀x😀"));
        assertEquals(Map.of(), TextSearch.countAll("abc", List.of()));
        assertEquals(Map.of("abcd", 0), TextSearch.countAll("abc", List.of("abcd")));
        assertEquals(Map.of("aa", 3), TextSearch.countAll("aaaa", List.of("aa", "aa")), "a repeated pattern counts once");
    }

    @Test
    void bad_input_is_refused() {
        assertThrows(IllegalArgumentException.class, () -> TextSearch.findAll("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.findAll("abc", null));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.findAll(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.prefixFunction(null));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.smallestPeriod(null));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.longestRepeated(null));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.countAll("abc", List.of("ab", "abc")));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.countAll("abc", List.of("")));
        assertThrows(IllegalArgumentException.class, () -> TextSearch.countAll(null, List.of("a")));
    }

    // ---- scale ----

    @Test
    void repetitive_text_and_pattern() {
        assertTimeoutPreemptively(LIMIT, () -> {
            String text = "a".repeat(400_000);
            String pattern = "a".repeat(20_000);
            List<Integer> all = TextSearch.findAll(text, pattern);
            assertEquals(380_001, all.size());
            assertEquals(379_999, (int) all.get(379_999));
            String almost = "a".repeat(20_000) + "b";
            for (int i = 0; i < 10; i++) assertTrue(TextSearch.findAll(text, almost).isEmpty());
            assertEquals(1, TextSearch.smallestPeriod(text));
            assertEquals(400_001, TextSearch.smallestPeriod(text + "b"));
        }, "findAll must be KMP: O(n + m), not a restart at every position");
    }

    @Test
    void many_long_patterns_of_one_length() {
        assertTimeoutPreemptively(LIMIT, () -> {
            Random random = new Random(2);
            String text = random(random, 1_000_000, 26);
            List<String> patterns = new ArrayList<>();
            for (int i = 0; i < 4_000; i++) {
                int at = random.nextInt(text.length() - 5_000);
                patterns.add(i % 2 == 0 ? text.substring(at, at + 5_000) : random(random, 5_000, 26));
            }
            Map<String, Integer> counts = TextSearch.countAll(text, patterns);
            assertEquals(Set.copyOf(patterns), counts.keySet());
            for (int i = 0; i < patterns.size(); i++) {
                if (i % 2 == 0) assertTrue(counts.get(patterns.get(i)) >= 1);
                else assertEquals(0, (int) counts.get(patterns.get(i)));
            }
        }, "countAll must slide one rolling hash over the text, not search each pattern or hash each window from scratch");
    }

    @Test
    void very_long_patterns() {
        assertTimeoutPreemptively(LIMIT, () -> {
            Random random = new Random(4);
            String text = random(random, 1_000_000, 26);
            List<String> patterns = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                int at = random.nextInt(text.length() - 60_000);
                patterns.add(text.substring(at, at + 60_000));
            }
            Map<String, Integer> counts = TextSearch.countAll(text, patterns);
            for (String p : patterns) assertEquals(1, (int) counts.get(p));
        }, "countAll must update the window hash in O(1) per step, not hash or copy each window");
    }

    @Test
    void longest_repeat_in_a_long_text() {
        assertTimeoutPreemptively(LIMIT, () -> {
            Random random = new Random(3);
            String text = random(random, 300_000, 26);
            String planted = random(random, 500, 26);
            String s = text.substring(0, 100_000) + planted + text.substring(100_000, 250_000) + planted + text.substring(250_000);
            assertEquals(planted, TextSearch.longestRepeated(s));
            String twice = random(random, 150_000, 4);
            assertEquals(twice, TextSearch.longestRepeated(twice + twice));
        }, "longestRepeated must binary search the length and check each length with rolling hashes");
    }
}
