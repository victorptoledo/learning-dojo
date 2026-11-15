# Heap and Trees

Two parts: a binary **min-heap** you write yourself, and a few classic **binary tree**
problems, some on binary search trees.

## Part 1: `MinHeap<T>`

A binary heap stored in an array: the children of index `i` are `2i + 1` and `2i + 2`, its parent
is `(i - 1) / 2`, and every parent is `<=` its children under the comparator. Do not use
`java.util.PriorityQueue` (an `ArrayList` or an `Object[]` for the storage is fine).

| Method | Does | Cost |
| --- | --- | --- |
| `new MinHeap<>(comparator)` | an empty heap | |
| `MinHeap.heapify(items, comparator)` | a heap of all the items, built **bottom-up** | O(n) |
| `add(item)` | appends, then sifts up | O(log n) |
| `peek()` | the smallest item | O(1) |
| `poll()` | removes the smallest: moves the last item to the root, then sifts down | O(log n) |
| `size()`, `isEmpty()` | | O(1) |

`peek` and `poll` on an empty heap throw `NoSuchElementException`.

**Bottom-up heapify** sifts down every non-leaf, from the last one (`n / 2 - 1`) to the root.
Most nodes are near the bottom and sift only a little, so the total is O(n), not O(n log n). The
hidden tests count the comparisons: building a heap of 100,000 items must use fewer than
300,000.

## Part 2: `Heaps`

- `largest(values, k)`: the `k` largest values, largest first. Keep a min-heap of at most `k`
  values: a new value replaces the smallest of them when it is bigger. O(n log k). `k` can be 0
  or larger than the array.
- `mergeSorted(arrays)`: merge sorted arrays into one sorted array. Keep a heap of one cursor
  per array, ordered by its current value. O(N log k) for N values in k arrays.

## Part 3: `Trees`

`TreeNode` (given) has an `int val` and `left` / `right` children. Trees in the hidden tests can
be **200,000 levels deep** (keys inserted in sorted order into a plain BST), so recursion would
overflow the stack: use an explicit stack (`ArrayDeque`) or a queue.

| Method | Returns |
| --- | --- |
| `isValidBst(root)` | whether every node is strictly greater than all of its left subtree and strictly smaller than all of its right subtree; `null` is valid |
| `kthSmallest(root, k)` | the k-th smallest value of a BST (k from 1); out of range throws `IllegalArgumentException` |
| `levelOrder(root)` | the values level by level, left to right: `[[1], [2, 3], [4]]` |
| `height(root)` | the number of levels; `null` has height 0 |
| `fromSorted(sorted)` | a **balanced** BST with these values: the middle (`(lo + hi) >>> 1`) is the root, recursively; height at most ⌊log₂ n⌋ + 1 |
| `lowestCommonAncestor(bstRoot, a, b)` | the value of the deepest node of the BST that has both `a` and `b` in its subtree (a node counts as its own descendant) |

> Tip: an in-order traversal of a BST visits the values in ascending order. With an explicit
> stack: go left as far as you can pushing nodes, pop one (that is the next value), then move to
> its right child and repeat. Both `isValidBst` and `kthSmallest` are that loop.
