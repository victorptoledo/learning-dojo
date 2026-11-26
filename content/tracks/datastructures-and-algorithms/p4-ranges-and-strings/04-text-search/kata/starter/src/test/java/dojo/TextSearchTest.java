package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TextSearchTest {
    @Test
    void prefix_function_and_overlapping_matches() {
        assertArrayEquals(new int[] {0, 0, 1, 2, 3, 0, 1}, TextSearch.prefixFunction("ababaca"));
        assertEquals(List.of(0, 1, 2), TextSearch.findAll("aaaa", "aa"));
        assertEquals(List.of(2, 4), TextSearch.findAll("xyababab", "abab"));
    }

    @Test
    void periods() {
        assertEquals(3, TextSearch.smallestPeriod("abcabcabc"));
        assertEquals(5, TextSearch.smallestPeriod("abcab"));
        assertEquals(1, TextSearch.smallestPeriod("zzzz"));
    }

    @Test
    void many_patterns_and_repeats() {
        assertEquals(Map.of("ana", 2, "nan", 1, "xyz", 0), TextSearch.countAll("banana", List.of("ana", "nan", "xyz")));
        assertEquals("ana", TextSearch.longestRepeated("banana"));
        assertEquals("", TextSearch.longestRepeated("abc"));
    }
}
