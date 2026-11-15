package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SearchTest {
    @Test
    void the_examples_of_the_readme() {
        assertEquals(1, Search.lowerBound(new int[] {1, 3, 3, 3, 8}, 3));
        assertEquals(3, Search.searchRotated(new int[] {30, 40, 50, 10, 20}, 10));
        assertEquals(6, Search.minShipCapacity(new int[] {3, 2, 2, 4, 1, 4}, 3));
        assertEquals(3, Search.kthSmallest(new int[] {5, 1, 3, 2, 4}, 3));
        assertEquals(3, Search.countInversions(new int[] {2, 4, 1, 3, 5}));
        assertEquals(
                List.of(List.of(2, 2, 2, 2), List.of(2, 3, 3), List.of(3, 5)),
                Search.combinationSum(new int[] {2, 3, 5}, 8));
    }
}
