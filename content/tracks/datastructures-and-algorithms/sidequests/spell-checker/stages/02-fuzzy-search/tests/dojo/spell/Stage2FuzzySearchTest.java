package dojo.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntBiFunction;
import org.junit.jupiter.api.Test;

class Stage2FuzzySearchTest {

    static SpellChecker readmeDictionary() {
        var spell = new SpellChecker();
        spell.add("the", 500);
        spell.add("then", 40);
        spell.add("they", 30);
        spell.add("tea", 5);
        spell.add("ten", 5);
        spell.add("to", 300);
        return spell;
    }

    /** Every word against the query: the slow way, for small dictionaries. */
    static List<String> slowSuggest(Map<String, Long> words, String word, int max, int limit,
            ToIntBiFunction<String, String> distance) {
        record Hit(String word, int distance, long frequency) {
        }
        var hits = new ArrayList<Hit>();
        words.forEach((w, f) -> {
            int d = distance.applyAsInt(word, w);
            if (d <= max) {
                hits.add(new Hit(w, d, f));
            }
        });
        hits.sort(Comparator.comparingInt(Hit::distance)
                .thenComparing(Comparator.comparingLong(Hit::frequency).reversed())
                .thenComparing(Hit::word));
        return hits.stream().limit(limit).map(Hit::word).toList();
    }

    @Test
    void theReadmeExample() {
        var spell = readmeDictionary();
        assertEquals(List.of("the"), spell.suggest("thw", 1, 10));
        assertEquals(List.of("the", "to", "then"), spell.suggest("thw", 2, 3));
    }

    @Test
    void ordersByDistanceThenFrequencyThenWord() {
        var spell = readmeDictionary();
        assertEquals(List.of("the", "to", "then", "they", "tea", "ten"), spell.suggest("THW", 2, 10));
        assertEquals(List.of("ten", "then"), spell.suggest("ten", 1, 2), "then and tea are at 1: then is used more");
        assertEquals(List.of("ten"), spell.suggest("ten", 0, 5), "distance 0 is the word itself");
        assertEquals(List.of(), spell.suggest("ten", 3, 0));
        assertEquals(List.of(), spell.suggest("xyzzy", 1, 5));
    }

    @Test
    void wordsMuchShorterOrLongerAreFoundToo() {
        var spell = new SpellChecker();
        spell.add("a");
        spell.add("abcdef");
        spell.add("abc");
        assertEquals(List.of("a", "abc"), spell.suggest("ab", 1, 5), "the same distance and frequency: alphabetical");
        assertEquals(List.of("abcdef"), spell.suggest("abcdefgh", 2, 5));
        assertEquals(List.of("a", "abc", "abcdef"), spell.suggest("a", 5, 5));
    }

    @Test
    void badQueriesAreRefused() {
        var spell = readmeDictionary();
        assertThrows(IllegalArgumentException.class, () -> spell.suggest(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> spell.suggest("", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> spell.suggest("t-o", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> spell.suggest("to", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> spell.suggest("to", 1, -1));
    }

    @Test
    void randomDictionariesMatchEveryWordAgainstTheQuery() {
        var random = new Random(71);
        for (int round = 0; round < 40; round++) {
            var spell = new SpellChecker();
            var words = new HashMap<String, Long>();
            for (int i = 0; i < 300; i++) {
                String w = Stage1DictionaryTest.randomWord(random, 1, 7, 4);
                long f = 1 + random.nextInt(5);
                spell.add(w, f);
                words.merge(w, f, Long::sum);
            }
            for (int query = 0; query < 30; query++) {
                String word = Stage1DictionaryTest.randomWord(random, 1, 8, 5);
                int max = random.nextInt(4);
                int limit = random.nextInt(12);
                assertEquals(slowSuggest(words, word, max, limit, Stage1DictionaryTest::slowDistance),
                        spell.suggest(word, max, limit), "suggest " + word + " within " + max + ", " + limit);
            }
        }
    }

    @Test
    void suggestionsPruneTheTrie() {
        var random = new Random(72);
        var spell = new SpellChecker();
        var words = new ArrayList<String>();
        for (int i = 0; i < 60_000; i++) {
            String w = Stage1DictionaryTest.randomWord(random, 6, 14, 26);
            words.add(w);
            spell.add(w, 1 + i % 7);
        }
        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            int withSuggestions = 0;
            for (int query = 0; query < 500; query++) {
                var typo = new StringBuilder(words.get(random.nextInt(words.size())));
                typo.setCharAt(random.nextInt(typo.length()), (char) ('a' + random.nextInt(26)));
                typo.insert(random.nextInt(typo.length() + 1), 'q');
                var found = spell.suggest(typo.toString(), 2, 5);
                if (!found.isEmpty()) {
                    withSuggestions++;
                }
            }
            assertTrue(withSuggestions == 500, "every typo is 2 edits from a word: " + withSuggestions);
        }, "500 suggestions in 60,000 words: is the distance computed word by word?");
    }
}
