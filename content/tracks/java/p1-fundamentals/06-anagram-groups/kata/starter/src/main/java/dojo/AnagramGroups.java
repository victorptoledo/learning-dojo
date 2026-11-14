package dojo;

import java.util.List;
import java.util.Map;

/** Groups words that are anagrams of each other. */
public final class AnagramGroups {
    private AnagramGroups() {}

    /** The lowercased, sorted letters of {@code word}. */
    public static String key(String word) {
        throw new UnsupportedOperationException("todo key of " + word);
    }

    /** Key to words, in order of first appearance; unmodifiable. */
    public static Map<String, List<String>> group(List<String> words) {
        throw new UnsupportedOperationException("todo group " + words.size() + " words");
    }

    /** Groups of two or more words, sorted inside, biggest first. */
    public static List<List<String>> families(List<String> words) {
        throw new UnsupportedOperationException("todo families of " + words.size() + " words");
    }
}
