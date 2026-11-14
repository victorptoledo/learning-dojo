package dojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Groups words that are anagrams of each other. */
public final class AnagramGroups {
    private AnagramGroups() {}

    /** The lowercased, sorted letters of {@code word}. */
    public static String key(String word) {
        char[] letters = word.strip().toLowerCase().toCharArray();
        Arrays.sort(letters);
        return new String(letters);
    }

    /** Key to words, in order of first appearance; unmodifiable. */
    public static Map<String, List<String>> group(List<String> words) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (String raw : words) {
            String word = raw.strip();
            if (word.isEmpty() || !seen.add(word.toLowerCase())) {
                continue;
            }
            groups.computeIfAbsent(key(word), k -> new ArrayList<>()).add(word);
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        groups.forEach((key, group) -> result.put(key, List.copyOf(group)));
        return java.util.Collections.unmodifiableMap(result);
    }

    /** Groups of two or more words, sorted inside, biggest first. */
    public static List<List<String>> families(List<String> words) {
        List<List<String>> families = new ArrayList<>();
        for (List<String> group : group(words).values()) {
            if (group.size() >= 2) {
                List<String> sorted = new ArrayList<>(group);
                sorted.sort(Comparator.naturalOrder());
                families.add(List.copyOf(sorted));
            }
        }
        families.sort(Comparator.<List<String>>comparingInt(List::size).reversed()
                .thenComparing(List::getFirst));
        return List.copyOf(families);
    }
}
