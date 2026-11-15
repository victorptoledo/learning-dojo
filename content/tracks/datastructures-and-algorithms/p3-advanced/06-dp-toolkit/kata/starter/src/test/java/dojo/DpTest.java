package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class DpTest {
    @Test
    void the_examples_of_the_readme() {
        assertEquals(6, Dp.gridPaths(new boolean[3][3]));
        assertEquals(3, Dp.editDistance("kitten", "sitting"));
        assertEquals(4, Dp.longestIncreasingSubsequence(new int[] {10, 9, 2, 5, 3, 7, 101, 18}));
        assertEquals(9, Dp.knapsack(new int[] {1, 3, 4, 5}, new int[] {1, 4, 5, 7}, 7));
        assertEquals(2, Dp.minCoins(new int[] {1, 3, 4}, 6));
        assertTrue(Dp.wordBreak("applepenapple", Set.of("apple", "pen")));
    }
}
