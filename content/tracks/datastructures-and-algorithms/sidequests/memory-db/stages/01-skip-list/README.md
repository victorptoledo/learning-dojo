A database index is a sorted map. Write one: `OrderedIndex<K, V>`, ordered by the `Comparator`
given to the constructor (a `null` comparator is an `IllegalArgumentException`).

| Method | Does |
| --- | --- |
| `V put(K key, V value)` | Stores the value and returns the one it replaced, or `null`. |
| `V get(K key)`, `V remove(K key)` | The value, or `null` when the key is absent; `remove` also takes it out. |
| `int size()` | How many keys. |
| `K first()`, `K last()` | The smallest and largest key, or `null` when empty. |
| `K floor(K key)`, `K ceiling(K key)` | The largest key `<= key`, the smallest key `>= key`, or `null`. |

A `null` key is an `IllegalArgumentException` everywhere. Do not use `TreeMap`, `TreeSet`, the
`ConcurrentSkipList` classes or a `PriorityQueue`: the tests walk your objects and fail if they
find one.

**The comparison budget.** The tests give you a `Comparator` that counts its calls and require,
on average, at most `3·log2(n) + 10` comparisons per `get`, `put`, `remove`, `floor` and
`ceiling` with 100,000 keys, inserted in random order **and in sorted order**. A plain binary
search tree meets the budget on random keys and fails on sorted ones (it becomes a list). A
sorted array meets the comparison budget, but each insertion shifts half the array, and the time
limit catches it.

**A skip list** is the simplest structure that holds the budget. It is a sorted linked list with
express lanes: every node is in level 0, and a node in level `i` is also in level `i + 1` with
probability 1/2 (flip a coin when you insert it). A search starts at the top level of a head node,
moves right while the next key is smaller, and drops a level when it is not; it takes about two
steps per level and about `log2(n)` levels. To insert, remember the last node you passed on each
level and link the new node after them; to remove, unlink it from the same nodes.

```text
level 2: head ------------------> 30 ---------------------> nil
level 1: head ------> 10 -------> 30 -------> 50 ---------> nil
level 0: head -> 5 -> 10 -> 20 -> 30 -> 40 -> 50 -> 60 ----> nil
```

A treap, an AVL or red-black tree, or a B-tree also passes. The next stage asks for `rank` and
`select` in O(log n), so a structure that can also count what lies to its left helps.
