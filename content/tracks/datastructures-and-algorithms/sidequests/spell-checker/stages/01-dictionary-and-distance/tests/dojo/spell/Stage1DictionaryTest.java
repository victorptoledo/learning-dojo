package dojo.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class Stage1DictionaryTest {

    /** The full table, for short words only. */
    static int slowDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0 || j == 0) {
                    d[i][j] = i + j;
                } else {
                    d[i][j] = Math.min(d[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                            Math.min(d[i - 1][j], d[i][j - 1]) + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    static String randomWord(Random random, int minLength, int maxLength, int alphabet) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        var out = new StringBuilder();
        for (int i = 0; i < length; i++) {
            out.append((char) ('a' + random.nextInt(alphabet)));
        }
        return out.toString();
    }

    @Test
    void theReadmeExample() {
        var spell = new SpellChecker();
        spell.add("the", 500);
        spell.add("cat");
        assertEquals(List.of(new Misspelling("catt", 4), new Misspelling("sat", 9)), spell.check("The catt sat."));
        assertEquals(3, SpellChecker.distance("kitten", "sitting"));
    }

    @Test
    void wordsIgnoreCaseAndAddUpTheirFrequencies() {
        var spell = new SpellChecker();
        spell.add("Hello", 3);
        spell.add("hello");
        spell.add("HELLO", 6);
        spell.add("help");
        assertEquals(2, spell.size());
        assertEquals(10, spell.frequency("hElLo"));
        assertTrue(spell.contains("HELP"));
        assertFalse(spell.contains("hel"), "a prefix is not a word");
        assertFalse(spell.contains("helpful"));
        assertFalse(spell.contains("he1p"), "not a word at all, but no exception");
        assertEquals(0, spell.frequency(""));
        assertEquals(0, spell.frequency("café"));
    }

    @Test
    void badWordsAreRefused() {
        var spell = new SpellChecker();
        assertThrows(IllegalArgumentException.class, () -> spell.add(null));
        assertThrows(IllegalArgumentException.class, () -> spell.add(""));
        assertThrows(IllegalArgumentException.class, () -> spell.add("two words"));
        assertThrows(IllegalArgumentException.class, () -> spell.add("don't"));
        assertThrows(IllegalArgumentException.class, () -> spell.add("naïve"));
        assertThrows(IllegalArgumentException.class, () -> spell.add("ok", 0));
        assertThrows(IllegalArgumentException.class, () -> spell.contains(null));
        assertThrows(IllegalArgumentException.class, () -> spell.frequency(null));
        assertThrows(IllegalArgumentException.class, () -> spell.check(null));
        assertThrows(IllegalArgumentException.class, () -> SpellChecker.distance(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> SpellChecker.distance("a", null));
        assertEquals(0, spell.size());
    }

    @Test
    void checkSplitsOnAnythingButLetters() {
        var spell = new SpellChecker();
        for (String w : List.of("it", "is", "a", "test")) {
            spell.add(w);
        }
        assertEquals(List.of(), spell.check("It is a TEST!"));
        assertEquals(List.of(), spell.check(""));
        assertEquals(List.of(), spell.check("  ...,,, 123 "));
        assertEquals(List.of(new Misspelling("Tset", 0), new Misspelling("isnt", 9), new Misspelling("s", 17), new Misspelling("t", 19)),
                spell.check("Tset-it, isnt it's t3"), "digits and quotes split words too");
    }

    @Test
    void distancesOfKnownPairs() {
        assertEquals(0, SpellChecker.distance("", ""));
        assertEquals(4, SpellChecker.distance("", "abcd"));
        assertEquals(4, SpellChecker.distance("abcd", ""));
        assertEquals(0, SpellChecker.distance("same", "same"));
        assertEquals(2, SpellChecker.distance("recieve", "receive"), "a swap costs two substitutions here");
        assertEquals(2, SpellChecker.distance("flaw", "lawn"));
        assertEquals(3, SpellChecker.distance("saturday", "sunday"));
        assertEquals(SpellChecker.distance("abc", "yabd"), SpellChecker.distance("yabd", "abc"));
    }

    @Test
    void randomDistancesMatchTheFullTable() {
        var random = new Random(61);
        for (int i = 0; i < 3_000; i++) {
            String a = randomWord(random, 0, 12, 3);
            String b = randomWord(random, 0, 12, 3);
            assertEquals(slowDistance(a, b), SpellChecker.distance(a, b), a + " / " + b);
        }
    }

    @Test
    void longStringsFitInTwoRows() {
        var random = new Random(62);
        String a = randomWord(random, 15_000, 15_000, 4);
        var b = new StringBuilder(a);
        for (int i = 0; i < 300; i++) {
            b.setCharAt(random.nextInt(b.length()), 'z');
        }
        b.insert(7_000, "zzzzz");
        String other = b.toString();
        assertTimeoutPreemptively(Duration.ofSeconds(8), () -> {
            try {
                int d = SpellChecker.distance(a, other);
                assertTrue(d >= 5 && d <= 305, "distance " + d);
                assertEquals(15_000, SpellChecker.distance(a, ""));
            } catch (OutOfMemoryError e) {
                throw new AssertionError("a 15,000 x 15,005 table does not fit in 256 MB: keep two rows");
            }
        });
    }

    @Test
    void aLargeDictionaryLooksWordsUpQuickly() {
        var random = new Random(63);
        var spell = new SpellChecker();
        var words = new String[100_000];
        for (int i = 0; i < words.length; i++) {
            words[i] = randomWord(random, 3, 12, 26);
        }
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (String w : words) {
                spell.add(w, 1 + (w.length() & 3));
            }
            int known = 0;
            for (int i = 0; i < 300_000; i++) {
                if (spell.contains(words[i % words.length])) {
                    known++;
                }
            }
            assertEquals(300_000, known);
            var text = new StringBuilder();
            for (int i = 0; i < 50_000; i++) {
                // longer than any word of the dictionary, so never a word by chance
                text.append(words[i]).append(i % 10 == 0 ? "qqqqqqqqqqqqq, " : " ");
            }
            assertEquals(5_000, spell.check(text.toString()).size());
        });
    }
}
