package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoutesTest {
    @Test
    void union_find_merges_sets() {
        var sets = new UnionFind(4);
        assertTrue(sets.union(0, 1));
        assertTrue(sets.connected(0, 1));
        assertEquals(3, sets.components());
    }

    @Test
    void small_graphs() {
        assertEquals(6, Routes.gridDistance(new String[] {"S.#.", ".##E", "...."}));
        int[][] edges = {{0, 1, 4}, {0, 2, 1}, {2, 1, 2}, {1, 3, 1}};
        assertArrayEquals(new long[] {0, 3, 1, 4}, Routes.shortestDistances(4, edges, 0));
        assertEquals(List.of(0, 2, 1, 3), Routes.shortestPath(4, edges, 0, 3));
        assertEquals(4, Routes.minimumSpanningTree(4, edges));
    }
}
