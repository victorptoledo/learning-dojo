package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnagramGroupsGradingTest {
    @Test
    void key_ignores_surrounding_spaces_and_case() {
        assertEquals("eilnst", AnagramGroups.key("  SiLeNt "));
        assertEquals("a", AnagramGroups.key("A"));
    }

    @Test
    void groups_anagrams_in_order_of_first_appearance() {
        var groups = AnagramGroups.group(List.of("listen", "google", "Silent", "enlist"));
        assertEquals(List.of("eilnst", "eggloo"), new ArrayList<>(groups.keySet()));
        assertEquals(List.of("listen", "Silent", "enlist"), groups.get("eilnst"));
        assertEquals(List.of("google"), groups.get("eggloo"));
    }

    @Test
    void skips_blank_words_and_repeats_ignoring_case() {
        var groups = AnagramGroups.group(List.of(" tea ", "", "ate", "TEA", "   ", "ate"));
        assertEquals(Map.of("aet", List.of("tea", "ate")), groups);
    }

    @Test
    void group_of_nothing_is_empty() {
        assertEquals(Map.of(), AnagramGroups.group(List.of()));
    }

    @Test
    void the_map_and_its_lists_cannot_be_changed() {
        var groups = AnagramGroups.group(List.of("tea", "eat"));
        assertThrows(UnsupportedOperationException.class, () -> groups.put("x", List.of()));
        var teas = groups.get("aet");
        assertThrows(UnsupportedOperationException.class, () -> teas.add("ate"));
    }

    @Test
    void families_keep_groups_of_two_or_more_biggest_first() {
        var families = AnagramGroups.families(List.of("tea", "listen", "ate", "silent", "eat", "dojo"));
        assertEquals(List.of(List.of("ate", "eat", "tea"), List.of("listen", "silent")), families);
    }

    @Test
    void families_of_the_same_size_are_ordered_by_first_word() {
        var families = AnagramGroups.families(List.of("stop", "dog", "pots", "god"));
        assertEquals(List.of(List.of("dog", "god"), List.of("pots", "stop")), families);
    }

    @Test
    void no_families_without_anagrams() {
        assertEquals(List.of(), AnagramGroups.families(List.of("one", "two", "three")));
    }
}
