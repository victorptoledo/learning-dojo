package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LogIndexGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(6);

    private static String random(Random random, int length, int alphabet) {
        char[] c = new char[length];
        for (int i = 0; i < length; i++) c[i] = (char) ('a' + random.nextInt(alphabet));
        return new String(c);
    }

    private static long slowCount(String text, String p) {
        long found = 0;
        for (int i = 0; i + p.length() <= text.length(); i++) if (text.startsWith(p, i)) found++;
        return found;
    }

    @Test
    void every_answer_matches_a_slow_model() {
        Random random = new Random(1);
        for (int round = 0; round < 150; round++) {
            var log = new LogIndex();
            List<String> model = new ArrayList<>();
            List<String> patterns = new ArrayList<>();
            for (int step = 0; step < 60; step++) {
                int action = random.nextInt(10);
                if (action == 0 || patterns.isEmpty()) {
                    String p = random(random, 1 + random.nextInt(3), 2);
                    log.watch(p);
                    if (!patterns.contains(p)) patterns.add(p);
                } else if (action < 5 || model.isEmpty()) {
                    String line = random(random, random.nextInt(12), 2);
                    assertEquals(model.size(), log.append(line));
                    model.add(line);
                } else if (action < 7) {
                    int at = random.nextInt(model.size());
                    String line = random(random, random.nextInt(12), 2);
                    log.replace(at, line);
                    model.set(at, line);
                }
                assertEquals(model.size(), log.lines());
                String p = patterns.get(random.nextInt(patterns.size()));
                int from = random.nextInt(model.size() + 1);
                int to = from + random.nextInt(model.size() - from + 1);
                long expected = 0;
                for (int i = from; i < to; i++) expected += slowCount(model.get(i), p);
                assertEquals(expected, log.count(p, from, to), "count " + p + " in " + model.subList(from, to));
                int first = -1;
                for (int i = from; i < model.size(); i++) {
                    if (slowCount(model.get(i), p) > 0) {
                        first = i;
                        break;
                    }
                }
                assertEquals(first, log.firstLineWith(p, from), "firstLineWith " + p + " from " + from + " in " + model);
                if (from < to) {
                    int busiest = from;
                    for (int i = from; i < to; i++) if (slowCount(model.get(i), p) > slowCount(model.get(busiest), p)) busiest = i;
                    assertEquals(busiest, log.busiestLine(p, from, to), "busiestLine " + p + " in " + model.subList(from, to));
                }
                if (!model.isEmpty()) {
                    int i = random.nextInt(model.size());
                    assertEquals(model.get(i), log.line(i));
                }
            }
        }
    }

    @Test
    void a_pattern_watched_late_counts_the_lines_already_there() {
        var log = new LogIndex();
        for (int i = 0; i < 100; i++) log.append(i % 7 == 0 ? "xyxyx" : "zzz");
        log.watch("xyx");
        assertEquals(15 * 2, log.count("xyx", 0, 100));
        assertEquals(0, log.firstLineWith("xyx", 0));
        assertEquals(7, log.firstLineWith("xyx", 1));
        assertEquals(-1, log.firstLineWith("xyx", 99));
        assertEquals(-1, log.firstLineWith("xyx", 100));
        log.watch("xyx"); // again: nothing changes
        assertEquals(30, log.count("xyx", 0, 100));
        assertEquals(0, log.busiestLine("xyx", 0, 100));
        assertEquals(14, log.busiestLine("xyx", 8, 100));
        assertEquals(8, log.busiestLine("xyx", 8, 14), "all zero: the first line of the range");
    }

    @Test
    void matches_overlap_but_never_cross_lines() {
        var log = new LogIndex();
        log.watch("abab");
        log.watch("ab");
        log.append("ababab");
        log.append("ab");
        log.append("ab");
        assertEquals(2, log.count("abab", 0, 3));
        assertEquals(5, log.count("ab", 0, 3));
        log.replace(0, "");
        assertEquals(0, log.count("abab", 0, 3));
        assertEquals(-1, log.firstLineWith("abab", 0));
        assertEquals("", log.line(0));
        assertThrows(IllegalArgumentException.class, () -> log.append("ab\nab"));
        assertThrows(IllegalArgumentException.class, () -> log.replace(1, "x\ny"));
        assertEquals(3, log.lines());
        assertEquals("ab", log.line(1));
    }

    @Test
    void bad_input_is_refused() {
        var log = new LogIndex();
        log.watch("e");
        log.append("e");
        log.append("ee");
        assertThrows(IllegalArgumentException.class, () -> log.watch(""));
        assertThrows(IllegalArgumentException.class, () -> log.watch(null));
        assertThrows(IllegalArgumentException.class, () -> log.count("f", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> log.firstLineWith(null, 0));
        assertThrows(IllegalArgumentException.class, () -> log.busiestLine("f", 0, 1));
        assertThrows(IllegalArgumentException.class, () -> log.busiestLine("e", 1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> log.count("e", 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> log.count("e", 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> log.count("e", -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> log.firstLineWith("e", 3));
        assertThrows(IndexOutOfBoundsException.class, () -> log.busiestLine("e", 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> log.replace(2, "e"));
        assertThrows(IndexOutOfBoundsException.class, () -> log.line(2));
        assertThrows(NullPointerException.class, () -> log.append(null));
        assertEquals(3, log.count("e", 0, 2));
        assertEquals(0, log.count("e", 1, 1));
    }

    // ---- scale ----

    @Test
    void range_counts_over_a_large_log() {
        assertTimeoutPreemptively(LIMIT, () -> {
            Random random = new Random(2);
            var log = new LogIndex();
            for (String p : List.of("ERROR", "WARN", "timeout", "user=42", "ab")) log.watch(p);
            String[] words = {"ERROR", "WARN", "INFO", "timeout", "user=42", "user=7", "ok", "ab", "ba"};
            int n = 200_000;
            for (int i = 0; i < n; i++) {
                StringBuilder line = new StringBuilder();
                for (int w = 0; w < 6; w++) line.append(words[random.nextInt(words.length)]).append(' ');
                log.append(line.toString());
            }
            long total = 0;
            for (int q = 0; q < 300_000; q++) {
                int from = random.nextInt(n / 4);
                int to = n - random.nextInt(n / 4);
                total += log.count("ERROR", from, to);
                if (q % 3 == 0) log.replace(random.nextInt(n), "ERROR ab ERROR");
                total += log.busiestLine("timeout", from, to);
            }
            assertTrue(total > 0);
        }, "count and busiestLine must be O(log n) per question: a Fenwick tree and a segment tree");
    }

    @Test
    void rare_matches_are_found_without_a_scan() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var log = new LogIndex();
            log.watch("PANIC");
            int n = 300_000;
            for (int i = 0; i < n; i++) log.append("INFO all good " + i);
            log.replace(n - 1, "PANIC at the end");
            Random random = new Random(3);
            for (int q = 0; q < 200_000; q++) {
                assertEquals(n - 1, log.firstLineWith("PANIC", random.nextInt(n)));
            }
            log.replace(n - 1, "calm again");
            assertEquals(-1, log.firstLineWith("PANIC", 0));
        }, "firstLineWith must search the Fenwick tree by prefix, not scan the lines");
    }

    @Test
    void repetitive_lines_are_counted_with_kmp() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var log = new LogIndex();
            log.watch("a".repeat(5_000) + "b");
            log.watch("a".repeat(5_000));
            for (int i = 0; i < 12; i++) log.append("a".repeat(150_000));
            assertEquals(0, log.count("a".repeat(5_000) + "b", 0, 12));
            assertEquals(12L * 145_001, log.count("a".repeat(5_000), 0, 12));
        }, "counting must be KMP, O(line + pattern), not a search restarted at every position");
    }

    @Test
    void many_appends_keep_append_cheap() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var log = new LogIndex();
            log.watch("x");
            log.watch("yy");
            for (int i = 0; i < 1_000_000; i++) log.append(i % 1_000 == 0 ? "xyyx" : "z");
            assertEquals(2_000, log.count("x", 0, 1_000_000));
            assertEquals(1_000, log.count("yy", 0, 1_000_000));
            assertEquals(999_000, log.firstLineWith("x", 998_001));
        }, "appending must not rebuild the trees each time: grow them by doubling");
    }
}
