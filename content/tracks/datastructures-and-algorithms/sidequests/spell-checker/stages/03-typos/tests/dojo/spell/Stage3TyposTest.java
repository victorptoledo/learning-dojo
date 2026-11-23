package dojo.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage3TyposTest {

    /** The full optimal string alignment table, for short words only. */
    static int slowTypoDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0 || j == 0) {
                    d[i][j] = i + j;
                    continue;
                }
                d[i][j] = Math.min(d[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                        Math.min(d[i - 1][j], d[i][j - 1]) + 1);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    @Test
    void theReadmeExample() {
        assertEquals(1, SpellChecker.typoDistance("teh", "the"));
        assertEquals(2, SpellChecker.distance("teh", "the"));
        assertEquals(3, SpellChecker.typoDistance("ca", "abc"));
        var spell = Stage2FuzzySearchTest.readmeDictionary();
        assertEquals(Optional.of("the"), spell.correct("Teh"));
        assertEquals(Optional.empty(), spell.correct("xqzv"));
    }

    @Test
    void knownTypoDistances() {
        assertEquals(1, SpellChecker.typoDistance("recieve", "receive"));
        assertEquals(1, SpellChecker.typoDistance("form", "from"));
        assertEquals(2, SpellChecker.typoDistance("abcd", "badc"), "two separate swaps");
        assertEquals(0, SpellChecker.typoDistance("", ""));
        assertEquals(3, SpellChecker.typoDistance("", "abc"));
        assertEquals(SpellChecker.distance("kitten", "sitting"), SpellChecker.typoDistance("kitten", "sitting"));
        assertThrows(IllegalArgumentException.class, () -> SpellChecker.typoDistance(null, "a"));
    }

    @Test
    void swapsChangeTheRanking() {
        var spell = new SpellChecker();
        spell.add("ten", 1_000);
        spell.add("the", 10);
        assertEquals(List.of("ten"), spell.suggest("teh", 1, 5), "Levenshtein: ten at 1, the at 2");
        assertEquals(List.of("ten", "the"), spell.suggestTypos("teh", 1, 5), "both at 1 now: ten by frequency");
        spell.add("the", 2_000);
        assertEquals(List.of("the", "ten"), spell.suggestTypos("teh", 1, 5));
    }

    @Test
    void correctPrefersKnownWordsAndTheBestTypo() {
        var spell = new SpellChecker();
        spell.add("receive", 50);
        spell.add("deceive", 80);
        spell.add("believe", 90);
        assertEquals(Optional.of("receive"), spell.correct("recieve"), "one swap beats two substitutions");
        assertEquals(Optional.of("believe"), spell.correct("BELIEVE"));
        assertEquals(Optional.of("deceive"), spell.correct("decieve"));
        assertEquals(Optional.empty(), spell.correct("abc"));
        assertThrows(IllegalArgumentException.class, () -> spell.correct("re-cieve"));
        assertThrows(IllegalArgumentException.class, () -> spell.correct(null));
        assertThrows(IllegalArgumentException.class, () -> spell.suggestTypos("ok", -1, 3));
    }

    @Test
    void randomTyposMatchTheFullTable() {
        var random = new Random(81);
        for (int i = 0; i < 3_000; i++) {
            String a = Stage1DictionaryTest.randomWord(random, 0, 10, 3);
            String b = Stage1DictionaryTest.randomWord(random, 0, 10, 3);
            assertEquals(slowTypoDistance(a, b), SpellChecker.typoDistance(a, b), a + " / " + b);
        }
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
                assertEquals(Stage2FuzzySearchTest.slowSuggest(words, word, max, limit, Stage3TyposTest::slowTypoDistance),
                        spell.suggestTypos(word, max, limit), "suggestTypos " + word + " within " + max + ", " + limit);
            }
        }
    }

    @Test
    void correctionsPruneTheTrie() {
        var random = new Random(82);
        var spell = new SpellChecker();
        var words = new ArrayList<String>();
        for (int i = 0; i < 60_000; i++) {
            String w = Stage1DictionaryTest.randomWord(random, 6, 14, 26);
            words.add(w);
            spell.add(w, 1 + i % 7);
        }
        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            for (int query = 0; query < 500; query++) {
                String original = words.get(random.nextInt(words.size()));
                var typo = new StringBuilder(original);
                int at = random.nextInt(typo.length() - 1);
                char c = typo.charAt(at);
                typo.setCharAt(at, typo.charAt(at + 1));
                typo.setCharAt(at + 1, c);
                // away from the swapped pair: the alignment never edits a swapped pair again
                if (at >= 1) {
                    typo.insert(0, 'q');
                } else {
                    typo.append('q');
                }
                var corrected = spell.correct(typo.toString());
                assertEquals(true, corrected.isPresent(), "a swap and an insertion: 2 typos from " + original);
                assertEquals(true, SpellChecker.typoDistance(typo.toString(), corrected.get()) <= 2);
            }
        }, "500 corrections in 60,000 words: is the distance computed word by word?");
    }
}
