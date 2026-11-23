A query like `WHERE price BETWEEN 10 AND 20 ORDER BY price LIMIT 5` walks the index from 10 and
stops after five entries, and `OFFSET 50000` or "what percentile is this score" asks for a
position. Add to `OrderedIndex`:

| Method | Does |
| --- | --- |
| `Iterator<Map.Entry<K, V>> range(K from, K to)` | The entries with `from <= key < to`, in order. A `null` bound means "from the start" or "to the end". It is **lazy**: it finds `from` in O(log n) and then steps one entry per `next()`. |
| `int rank(K key)` | How many keys are smaller than `key` (the key need not be in the index). `null` is an `IllegalArgumentException`. |
| `K select(int index)` | The key at position `index` in order, from 0. Outside `0..size()-1` is an `IndexOutOfBoundsException`. |

```java
// keys 10, 20, 30, 40, 50
index.range(20, 45);    // 20, 30, 40
index.range(null, 25);  // 10, 20
index.rank(30);         // 2
index.rank(35);         // 3
index.select(4);        // 50
```

The budget: creating a range and reading `k` entries costs at most `3·log2(n) + 2k + 10`
comparisons, and `rank` at most `3·log2(n) + 10`. `select` compares nothing, and the tests time
200,000 calls of `rank` and `select` on 200,000 keys: counting from the start is too slow.

**Spans.** In a skip list, store on every forward pointer how many level-0 nodes it jumps over
(its *span*). A search adds up the spans of the pointers it follows, which is the rank; `select`
follows the pointers whose spans still fit in the position it wants. On insert, the new node's
spans and those of the nodes before it change by how far apart they are (remember, per level, the
rank of the last node you passed); on remove, the spans before it shrink by one or absorb the
removed node's spans. In a balanced tree or a treap, the same comes from storing the size of each
subtree.
