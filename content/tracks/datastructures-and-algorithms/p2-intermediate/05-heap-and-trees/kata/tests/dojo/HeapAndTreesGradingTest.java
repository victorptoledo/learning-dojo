package dojo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class HeapAndTreesGradingTest {
    private static final Duration LIMIT = Duration.ofSeconds(4);

    // ---- heap ----

    @Test
    void random_operations_match_a_priority_queue() {
        var random = new Random(8);
        var expected = new PriorityQueue<Integer>();
        var heap = new MinHeap<Integer>(Comparator.naturalOrder());
        for (int step = 0; step < 50_000; step++) {
            if (random.nextInt(3) > 0 || expected.isEmpty()) {
                int v = random.nextInt(100);
                expected.add(v);
                heap.add(v);
            } else {
                assertEquals(expected.poll(), heap.poll());
            }
            assertEquals(expected.size(), heap.size());
            if (!expected.isEmpty()) assertEquals(expected.peek(), heap.peek());
        }
    }

    @Test
    void an_empty_heap_throws_and_the_comparator_decides_the_order() {
        var heap = new MinHeap<String>(Comparator.comparing(String::length).reversed());
        assertThrows(NoSuchElementException.class, heap::peek);
        assertThrows(NoSuchElementException.class, heap::poll);
        heap.add("a");
        heap.add("ccc");
        heap.add("bb");
        assertEquals("ccc", heap.poll(), "the longest first under a reversed length order");
        assertEquals("bb", heap.poll());
        assertEquals("a", heap.poll());
        assertTrue(heap.isEmpty());
    }

    @Test
    void heapify_builds_a_valid_heap_in_linear_comparisons() {
        var comparisons = new AtomicLong();
        Comparator<Integer> counting = (a, b) -> {
            comparisons.incrementAndGet();
            return Integer.compare(a, b);
        };
        int n = 100_000;
        List<Integer> descending = IntStream.range(0, n).map(i -> n - i).boxed().toList();
        var heap = MinHeap.heapify(descending, counting);
        assertTrue(comparisons.get() < 3L * n, comparisons.get() + " comparisons to build a heap of " + n);
        assertEquals(n, heap.size());
        for (int i = 1; i <= 1_000; i++) assertEquals(i, heap.poll());
        var empty = MinHeap.heapify(List.<Integer>of(), counting);
        assertTrue(empty.isEmpty());
    }

    @Test
    void a_million_adds_and_polls_are_fast() {
        assertTimeoutPreemptively(LIMIT, () -> {
            var heap = new MinHeap<Integer>(Comparator.naturalOrder());
            var random = new Random(9);
            for (int i = 0; i < 1_000_000; i++) heap.add(random.nextInt());
            int previous = Integer.MIN_VALUE;
            while (!heap.isEmpty()) {
                int next = heap.poll();
                assertTrue(next >= previous);
                previous = next;
            }
        });
    }

    @Test
    void largest_values_come_largest_first() {
        var random = new Random(10);
        for (int round = 0; round < 300; round++) {
            int[] values = random.ints(random.nextInt(20), -50, 50).toArray();
            int k = random.nextInt(25);
            int[] before = values.clone();
            List<Integer> sortedDesc = new ArrayList<>(Arrays.stream(values).boxed().toList());
            sortedDesc.sort(Comparator.reverseOrder());
            assertEquals(sortedDesc.subList(0, Math.min(k, values.length)), Heaps.largest(values, k));
            assertArrayEquals(before, values);
        }
        int[] big = new Random(11).ints(3_000_000).toArray();
        assertTimeoutPreemptively(LIMIT, () -> assertEquals(10, Heaps.largest(big, 10).size()));
    }

    @Test
    void merge_sorted_merges_everything() {
        var random = new Random(12);
        List<int[]> arrays = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            arrays.add(random.ints(random.nextInt(400), -1_000, 1_000).sorted().toArray());
        }
        arrays.add(new int[0]);
        int[] expected = arrays.stream().flatMapToInt(Arrays::stream).sorted().toArray();
        int[] merged = assertTimeoutPreemptively(LIMIT, () -> Heaps.mergeSorted(arrays));
        assertArrayEquals(expected, merged);
        assertArrayEquals(new int[0], Heaps.mergeSorted(List.of()));
    }

    // ---- trees ----

    private static TreeNode insert(TreeNode root, int value) {
        var created = new TreeNode(value);
        if (root == null) return created;
        TreeNode node = root;
        while (true) {
            if (value < node.val) {
                if (node.left == null) {
                    node.left = created;
                    return root;
                }
                node = node.left;
            } else {
                if (node.right == null) {
                    node.right = created;
                    return root;
                }
                node = node.right;
            }
        }
    }

    private static TreeNode randomBst(Random random, int size) {
        TreeNode root = null;
        for (int v : random.ints(size * 3L, 0, size * 10).distinct().limit(size).toArray()) {
            root = insert(root, v);
        }
        return root;
    }

    private static void inorder(TreeNode node, List<Integer> out) {
        if (node == null) return;
        inorder(node.left, out);
        out.add(node.val);
        inorder(node.right, out);
    }

    @Test
    void valid_and_invalid_search_trees() {
        assertTrue(Trees.isValidBst(null));
        assertTrue(Trees.isValidBst(randomBst(new Random(13), 500)));
        //     5
        //    / \
        //   3   8
        //    \
        //     6     ← 6 is greater than 5 but in its left subtree
        var bad = new TreeNode(5, new TreeNode(3, null, new TreeNode(6)), new TreeNode(8));
        assertFalse(Trees.isValidBst(bad));
        assertFalse(Trees.isValidBst(new TreeNode(2, new TreeNode(2), null)), "duplicates are not strictly smaller");
        var extremes = new TreeNode(Integer.MIN_VALUE, null, new TreeNode(Integer.MAX_VALUE));
        assertTrue(Trees.isValidBst(extremes));
    }

    @Test
    void kth_smallest_and_lowest_common_ancestor_on_random_trees() {
        var random = new Random(14);
        for (int round = 0; round < 50; round++) {
            TreeNode root = randomBst(random, 1 + random.nextInt(60));
            var values = new ArrayList<Integer>();
            inorder(root, values);
            for (int k = 1; k <= values.size(); k++) assertEquals(values.get(k - 1), Trees.kthSmallest(root, k));
            assertThrows(IllegalArgumentException.class, () -> Trees.kthSmallest(root, values.size() + 1));
            assertThrows(IllegalArgumentException.class, () -> Trees.kthSmallest(root, 0));
            int a = values.get(random.nextInt(values.size()));
            int b = values.get(random.nextInt(values.size()));
            assertEquals(slowLca(root, a, b), Trees.lowestCommonAncestor(root, a, b));
        }
    }

    private static int slowLca(TreeNode root, int a, int b) {
        List<TreeNode> pathA = path(root, a);
        List<TreeNode> pathB = path(root, b);
        int i = 0;
        while (i < pathA.size() && i < pathB.size() && pathA.get(i) == pathB.get(i)) i++;
        return pathA.get(i - 1).val;
    }

    private static List<TreeNode> path(TreeNode root, int value) {
        var out = new ArrayList<TreeNode>();
        TreeNode node = root;
        while (node != null) {
            out.add(node);
            if (value == node.val) break;
            node = value < node.val ? node.left : node.right;
        }
        return out;
    }

    @Test
    void level_order_and_height() {
        //       1
        //      / \
        //     2   3
        //    /     \
        //   4       5
        var root = new TreeNode(1, new TreeNode(2, new TreeNode(4), null), new TreeNode(3, null, new TreeNode(5)));
        assertEquals(List.of(List.of(1), List.of(2, 3), List.of(4, 5)), Trees.levelOrder(root));
        assertEquals(3, Trees.height(root));
        assertEquals(List.of(), Trees.levelOrder(null));
        assertEquals(0, Trees.height(null));
    }

    @Test
    void from_sorted_is_balanced_and_in_order() {
        for (int n = 0; n <= 200; n++) {
            int[] sorted = IntStream.range(0, n).map(i -> 3 * i).toArray();
            TreeNode root = Trees.fromSorted(sorted);
            var values = new ArrayList<Integer>();
            inorder(root, values);
            assertEquals(Arrays.stream(sorted).boxed().toList(), values);
            int maxHeight = n == 0 ? 0 : 32 - Integer.numberOfLeadingZeros(n);
            assertTrue(Trees.height(root) <= maxHeight, "height " + Trees.height(root) + " for " + n);
            if (n > 0) assertEquals(sorted[(n - 1) >>> 1], root.val, "the middle is the root");
        }
    }

    @Test
    void deep_trees_do_not_overflow_the_stack() {
        int n = 200_000;
        TreeNode root = new TreeNode(0);
        TreeNode tail = root;
        for (int i = 1; i < n; i++) {
            tail.right = new TreeNode(i);
            tail = tail.right;
        }
        assertTrue(Trees.isValidBst(root));
        assertEquals(n - 1, Trees.kthSmallest(root, n));
        assertEquals(n, Trees.height(root));
        assertEquals(n, Trees.levelOrder(root).size());
        assertEquals(n - 2, Trees.lowestCommonAncestor(root, n - 2, n - 1));
        tail.right = new TreeNode(5);
        assertFalse(Trees.isValidBst(root));
    }
}
