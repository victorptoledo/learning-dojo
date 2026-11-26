package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RangeToolkitTest {
    @Test
    void range_adds() {
        long[] values = RangeToolkit.applyRangeAdds(6, new int[][] {{1, 4, 10}, {3, 6, 5}, {0, 1, -2}});
        assertArrayEquals(new long[] {-2, 10, 10, 15, 5, 5}, values);
    }

    @Test
    void fenwick_sums_updates_and_search() {
        var f = new RangeToolkit.Fenwick(new long[] {3, 0, 4, 1, 5});
        assertEquals(13, f.sum(0, 5));
        assertEquals(5, f.sum(2, 4));
        assertEquals(0, f.sum(3, 3));
        f.add(1, 2);                       // {3, 2, 4, 1, 5}
        assertEquals(9, f.sum(0, 3));
        assertEquals(0, f.firstReaching(3));
        assertEquals(1, f.firstReaching(4));
        assertEquals(4, f.firstReaching(15));
        assertEquals(-1, f.firstReaching(16));
    }

    @Test
    void sparse_min_and_max_tree() {
        long[] values = {5, 2, 8, 2, 9, 1};
        var sparse = new RangeToolkit.SparseMin(values);
        assertEquals(2, sparse.min(0, 4));
        assertEquals(1, sparse.min(2, 6));
        var tree = new RangeToolkit.MaxTree(values);
        assertEquals(9, tree.max(0, 6));
        assertEquals(2, tree.argMax(0, 4));
        tree.set(0, 9);
        assertEquals(0, tree.argMax(0, 6));  // ties go to the first position
    }
}
