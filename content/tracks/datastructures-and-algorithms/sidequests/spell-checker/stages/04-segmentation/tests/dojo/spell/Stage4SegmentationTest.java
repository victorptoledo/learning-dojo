package dojo.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Stage4SegmentationTest {

    private static SpellChecker dictionary(String... words) {
        var spell = new SpellChecker();
        for (String w : words) {
            spell.add(w);
        }
        return spell;
    }

    /** Every split, by recursion: exponential, for short texts only. */
    private static Optional<List<String>> slowSegment(Set<String> words, String text) {
        if (text.isEmpty()) {
            return Optional.of(List.of());
        }
        Optional<List<String>> best = Optional.empty();
        for (int j = text.length(); j >= 1; j--) {
            String head = text.substring(0, j);
            if (!words.contains(head)) {
                continue;
            }
            var rest = slowSegment(words, text.substring(j));
            if (rest.isPresent() && (best.isEmpty() || rest.get().size() + 1 < best.get().size())) {
                var split = new ArrayList<String>();
                split.add(head);
                split.addAll(rest.get());
                best = Optional.of(split);
            }
        }
        return best;
    }

    @Test
    void theReadmeExample() {
        var spell = dictionary("a", "an", "and", "android", "droid", "i", "the", "ant", "is");
        assertEquals(Optional.of(List.of("the", "android")), spell.segment("TheAndroid"));
        assertEquals(Optional.of(List.of("an", "android")), spell.segment("anandroid"));
        assertEquals(Optional.of(List.of("ant", "is")), spell.segment("antis"));
        assertEquals(Optional.empty(), spell.segment("xyz"));
    }

    @Test
    void edgesAndBadTexts() {
        var spell = dictionary("go", "goal", "al");
        assertEquals(Optional.of(List.of()), spell.segment(""));
        assertEquals(Optional.of(List.of("goal")), spell.segment("goal"), "one word beats go + al");
        assertEquals(Optional.of(List.of("go", "go", "goal")), spell.segment("gogogoal"));
        assertEquals(Optional.empty(), spell.segment("goa"));
        assertEquals(Optional.empty(), new SpellChecker().segment("a"));
        assertThrows(IllegalArgumentException.class, () -> spell.segment(null));
        assertThrows(IllegalArgumentException.class, () -> spell.segment("go goal"));
        assertThrows(IllegalArgumentException.class, () -> spell.segment("go1"));
    }

    @Test
    void tiesKeepTheLongestFirstWord() {
        var spell = dictionary("ab", "abc", "c", "cd", "d", "bcd");
        // ab+cd and abc+d both take two words: the longer first word wins
        assertEquals(Optional.of(List.of("abc", "d")), spell.segment("abcd"));
        var other = dictionary("x", "xy", "yz", "z", "xyz");
        assertEquals(Optional.of(List.of("xyz", "xy")), other.segment("xyzxy"));
    }

    @Test
    void randomTextsMatchEverySplit() {
        var random = new Random(91);
        for (int round = 0; round < 300; round++) {
            var words = new HashSet<String>();
            var spell = new SpellChecker();
            for (int i = 0; i < 12; i++) {
                String w = Stage1DictionaryTest.randomWord(random, 1, 4, 3);
                words.add(w);
                spell.add(w);
            }
            String text = Stage1DictionaryTest.randomWord(random, 0, 14, 3);
            assertEquals(slowSegment(words, text), spell.segment(text), "segment " + text + " with " + words);
        }
    }

    @Test
    void longTextsAreSplitInLinearTime() {
        var random = new Random(92);
        var spell = new SpellChecker();
        var words = new ArrayList<String>();
        for (int i = 0; i < 50_000; i++) {
            String w = Stage1DictionaryTest.randomWord(random, 2, 10, 26);
            words.add(w);
            spell.add(w);
        }
        var text = new StringBuilder();
        while (text.length() < 200_000) {
            text.append(words.get(random.nextInt(words.size())));
        }
        var hard = dictionary("a", "aa", "aaa", "aaaa", "aaaaa");
        String almost = "a".repeat(5_000) + "b";
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var split = spell.segment(text.toString()).orElseThrow();
            assertEquals(text.toString(), String.join("", split));
            assertTrue(split.size() <= text.length() / 2);
            assertEquals(Optional.empty(), hard.segment(almost), "plain recursion tries 2^5000 splits here");
            assertEquals(1_000, hard.segment("a".repeat(5_000)).orElseThrow().size());
        }, "200,000 letters: is every substring checked, or the same suffix split again and again?");
    }
}
