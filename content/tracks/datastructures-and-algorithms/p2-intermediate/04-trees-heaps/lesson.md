# Trees, Binary Search Trees and Heaps

Trees are everywhere you have hierarchy or order: file systems, the DOM, JSON, compilers' syntax
trees, database indexes, `TreeMap`, priority queues and schedulers. This lesson covers the
vocabulary, the traversals, the **binary search tree** with its balanced versions, and the
**heap**, a tree laid out in an array.

## Vocabulary

A **tree** is nodes connected by edges with one **root** and no cycles. Every node except the root
has one **parent**; nodes without children are **leaves**. The **depth** of a node is its
distance from the root; the **height** of the tree is the largest depth (plus one if you count
levels). A **binary** tree has at most two children per node.

| Shape | Meaning | Height for n nodes |
| --- | --- | --- |
| balanced | subtrees of every node have similar heights | O(log n) |
| complete | every level full except the last, filled from the left | ⌊log₂ n⌋ + 1 |
| degenerate | every node has one child: a linked list | n |

Almost every tree cost is O(height), so keeping trees shallow is the whole game.

## Traversals

```text
        8
       / \
      3   10           pre-order (node, left, right):  8 3 1 6 10 14
     / \    \          in-order  (left, node, right):  1 3 6 8 10 14
    1   6    14        post-order (left, right, node): 1 6 3 14 10 8
                       level order (by depth):          8 | 3 10 | 1 6 14
```

- **Pre-order**: copy or serialise a tree; print a directory listing.
- **In-order**: on a BST, the keys in sorted order.
- **Post-order**: compute something from the children first (sizes, heights, deleting a tree,
  evaluating an expression tree).
- **Level order**: a **BFS** with a queue; read `queue.size()` at the start of each level to
  group nodes by depth.

Recursive traversals use O(height) stack. For deep trees, use an explicit stack: for in-order,
push left children until you cannot, pop one (visit it), move to its right child, repeat.

## Binary search trees

A **BST** keeps, for every node, smaller keys in the left subtree and larger keys in the right
one. Search, insert and delete all walk one path from the root: **O(height)**.

- **Search**: go left or right by comparison.
- **Insert**: search, then hang the new node where the search fell off.
- **Delete**: a leaf goes away; a node with one child is replaced by it; a node with two children
  is replaced by its **in-order successor** (the smallest key of its right subtree).
- **Ordered queries** come for free: min and max, **floor** and **ceiling**, the next key, all
  keys in a range in O(log n + k).

Validating a BST is a classic trap: checking only `left.val < node.val < right.val` is not
enough. Each node must fit the bounds set by **all** its ancestors; an in-order traversal that
checks each key is greater than the previous one is the simplest correct test.

### Why balance matters, and how it is kept

Insert keys in sorted order into a plain BST and you get a chain: every operation is O(n).
**Self-balancing** trees fix the shape after each update with **rotations**:

| Tree | Guarantee | Where |
| --- | --- | --- |
| AVL | heights of siblings differ by at most 1: very balanced, fast lookups | in-memory indexes, some databases |
| Red-black | no path more than twice as long as another: fewer rotations on updates | `TreeMap`, `TreeSet`, `HashMap` treeified bins, the Linux scheduler (CFS) |
| B-tree / B+ tree | hundreds of keys per node, all leaves at the same depth | every relational database index, file systems |
| Skip list (not a tree, same job) | probabilistic O(log n) | Redis sorted sets, `ConcurrentSkipListMap` |

A **B+ tree** stores keys in nodes the size of a disk page (8–16 KB), so a table of a billion
rows needs about 4 levels: an index lookup reads 4 pages. Its leaves are linked, so a range scan
reads them in order. That is what `CREATE INDEX` builds.

## Heaps and priority queues

A **binary heap** is a complete binary tree where every parent is smaller than (min-heap) or
larger than (max-heap) its children. It does not keep everything sorted, only the **best element
at the root**. Being complete, it lives in an array with no pointers:

```text
index:     0  1  2  3  4  5
value:   [ 1, 3, 2, 7, 4, 5 ]       children of i: 2i+1, 2i+2      parent of i: (i-1)/2
```

| Operation | How | Cost |
| --- | --- | --- |
| peek | the root | O(1) |
| add | append, **sift up** while smaller than the parent | O(log n) |
| poll | move the last element to the root, **sift down** towards the smaller child | O(log n) |
| build from n items | sift down every non-leaf, bottom-up | O(n) |
| find or remove an arbitrary element | scan | O(n) |

Classic uses:

- **Top k of n**: a min-heap of size k; a new item replaces the root when larger. O(n log k).
- **Merge k sorted lists**: a heap of the current head of each list. O(N log k). External merge
  sort and LSM compaction work this way.
- **Running median**: a max-heap for the lower half and a min-heap for the upper half.
- **Scheduling**: timers (`ScheduledThreadPoolExecutor` keeps a heap of delayed tasks),
  Dijkstra's algorithm, event simulation.
- **Heapsort**: build, then poll n times.

If you need to change an element's priority, use an **indexed** heap (a map from element to its
position) or simply add the new version and skip stale ones when they come out ("lazy
deletion"), which the Dijkstra kata does.

## Choosing in Java

| Need | Structure |
| --- | --- |
| repeatedly take the smallest/largest | `PriorityQueue` (binary heap) |
| sorted keys, floor/ceiling, ranges, first/last | `TreeMap` / `TreeSet` (red-black) |
| the same, many threads | `ConcurrentSkipListMap` |
| k-th smallest of a static array | quickselect |
| rank of a key in a changing set | an order-statistic tree (subtree sizes), not in the JDK |

## Summary

- Tree costs are O(height); balanced trees keep height O(log n).
- Four traversals; in-order on a BST is sorted; level order is a BFS.
- Plain BSTs degrade on sorted input; red-black trees (`TreeMap`) and B-trees (database
  indexes) stay balanced.
- A heap gives the min or max in O(1) and updates in O(log n), built in O(n), but cannot search.
- Top-k, k-way merge, running median and schedulers are heap problems.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Binary Search Trees": <https://algs4.cs.princeton.edu/32bst/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Balanced Search Trees" (red-black trees): <https://algs4.cs.princeton.edu/33balanced/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Priority Queues": <https://algs4.cs.princeton.edu/24pq/>
- JDK 25 API, `PriorityQueue`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/PriorityQueue.html>
- JDK 25 API, `TreeMap`: <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/TreeMap.html>
- PostgreSQL documentation, "B-Tree Indexes": <https://www.postgresql.org/docs/current/btree.html>
- roadmap.sh, DSA topic "Binary Search Trees": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/binary-search-trees@UYm7unZfXd9Ge5EYV4odb.md>
- roadmap.sh, DSA topic "Heap": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/heap@R3Coq0TG1_cSOXzXWgpuI.md>
