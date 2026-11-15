package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeapAndTreesTest {
    @Test
    void the_heap_returns_items_in_order() {
        var heap = new MinHeap<Integer>(Comparator.naturalOrder());
        heap.add(5);
        heap.add(1);
        heap.add(3);
        assertEquals(1, heap.poll());
        assertEquals(3, heap.poll());
        assertEquals(5, heap.poll());
        assertTrue(heap.isEmpty());
    }

    @Test
    void a_small_tree() {
        //     2
        //    / \
        //   1   3
        var root = new TreeNode(2, new TreeNode(1), new TreeNode(3));
        assertTrue(Trees.isValidBst(root));
        assertEquals(List.of(List.of(2), List.of(1, 3)), Trees.levelOrder(root));
        assertEquals(List.of(9, 7), Heaps.largest(new int[] {4, 9, 1, 7}, 2));
    }
}
