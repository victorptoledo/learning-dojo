package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SequencesTest {
    @Test
    void the_examples_of_the_readme() {
        assertArrayEquals(new int[] {0, 4}, Sequences.pairWithSum(new int[] {1, 2, 4, 4, 7, 11}, 8));
        assertEquals(3, Sequences.longestUniqueRun("abcabcbb"));
        assertArrayEquals(new int[] {4, 2, 4, -1, -1}, Sequences.nextGreater(new int[] {2, 1, 2, 4, 3}));
        assertTrue(Sequences.balanced("f(a[i]) { return; }"));
        assertFalse(Sequences.balanced("([)]"));
        assertEquals(2, Sequences.countSubarraysWithSum(new int[] {1, 1, 1}, 2));
        assertEquals(2, Sequences.shortestWindowWithSum(new int[] {2, 3, 1, 2, 4, 3}, 7));
    }
}
