package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class AutocompleteGradingTest {

    private static String randomWord(Random random, int maxLength, int alphabet) {
        int length = 1 + random.nextInt(maxLength);
        var sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append((char) ('a' + random.nextInt(alphabet)));
        return sb.toString();
    }

    private static List<String> slowSuggest(Map<String, Long> words, String prefix, int k) {
        return words.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(k)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Distinct prefixes of all words: the nodes a pruned trie must have. */
    private static int slowNodeCount(Map<String, Long> words) {
        var prefixes = new HashSet<String>();
        for (String w : words.keySet()) {
            for (int i = 1; i <= w.length(); i++) prefixes.add(w.substring(0, i));
        }
        return prefixes.size();
    }

    @Test
    void random_operations_match_a_sorted_map() {
        var random = new Random(51);
        var expected = new TreeMap<String, Long>();
        var ac = new Autocomplete();
        for (int step = 0; step < 4_000; step++) {
            String word = randomWord(random, 5, 3);
            if (random.nextInt(4) == 0) {
                assertEquals(expected.remove(word) != null, ac.remove(word), "remove " + word);
            } else {
                long times = 1 + random.nextInt(3);
                expected.merge(word, times, Long::sum);
                ac.add(word, times);
            }
            String prefix = randomWord(random, 3, 3);
            prefix = prefix.substring(0, random.nextInt(prefix.length() + 1));
            assertEquals(expected.size(), ac.size());
            assertEquals(expected.getOrDefault(word, 0L), ac.frequency(word));
            assertEquals(expected.containsKey(word), ac.contains(word));
            String p = prefix;
            assertEquals(expected.keySet().stream().filter(w -> w.startsWith(p)).count(), ac.countWithPrefix(prefix), "prefix '" + prefix + "'");
            int k = random.nextInt(5);
            assertEquals(slowSuggest(expected, prefix, k), ac.suggest(prefix, k), "suggest '" + prefix + "' " + k);
            if (step % 50 == 0) assertEquals(slowNodeCount(expected), ac.nodeCount(), "node count after pruning");
        }
    }

    @Test
    void prefixes_that_are_words_and_words_that_are_prefixes() {
        var ac = new Autocomplete();
        ac.add("car");
        ac.add("cart");
        ac.add("carton");
        assertEquals(6, ac.nodeCount());
        assertTrue(ac.remove("cart"));
        assertEquals(6, ac.nodeCount(), "carton still needs the t");
        assertTrue(ac.contains("carton"));
        assertFalse(ac.contains("cart"));
        assertTrue(ac.remove("carton"));
        assertEquals(3, ac.nodeCount());
        assertFalse(ac.remove("carton"));
        assertFalse(ac.contains("ca"), "a prefix is not a word");
        assertEquals(1, ac.countWithPrefix(""));
        assertEquals(0, ac.countWithPrefix("x"));
        assertEquals(List.of(), ac.suggest("x", 3));
        assertEquals(List.of("car"), ac.suggest("", 10));
    }

    @Test
    void bad_input_is_refused() {
        var ac = new Autocomplete();
        assertThrows(IllegalArgumentException.class, () -> ac.add(""));
        assertThrows(IllegalArgumentException.class, () -> ac.add(null));
        assertThrows(IllegalArgumentException.class, () -> ac.add("a", 0));
        assertThrows(IllegalArgumentException.class, () -> ac.remove(""));
        assertThrows(IllegalArgumentException.class, () -> ac.countWithPrefix(null));
        assertThrows(IllegalArgumentException.class, () -> ac.suggest("a", -1));
        assertEquals(0, ac.size());
    }

    @Test
    void many_words_and_many_queries_are_fast() {
        var random = new Random(52);
        var words = new ArrayList<String>();
        for (int i = 0; i < 200_000; i++) words.add(randomWord(random, 12, 26));
        var ac = new Autocomplete();
        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            for (String w : words) ac.add(w, 1 + (w.hashCode() & 7));
            long total = 0;
            for (int i = 0; i < 200_000; i++) {
                String w = words.get(i);
                total += ac.countWithPrefix(w.substring(0, Math.min(3, w.length())));
            }
            assertTrue(total > 0);
            int suggested = 0;
            for (int i = 0; i < 100_000; i++) {
                String w = words.get(i);
                if (w.length() >= 6) suggested += ac.suggest(w.substring(0, 6), 5).size();
            }
            assertTrue(suggested > 0);
        });
        assertEquals(new HashSet<>(words).size(), ac.size());
        var top = ac.suggest("", 3);
        assertEquals(3, top.size());
        top.stream().map(ac::frequency).reduce((a, b) -> {
            assertTrue(a >= b, "most frequent first");
            return b;
        });
    }
}
