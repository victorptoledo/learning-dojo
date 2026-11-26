# Range Toolkit

Four structures from the lesson, each for a different kind of range question. All ranges are
**half-open**: `[lo, hi)` holds the positions `lo` to `hi - 1`, and `lo == hi` is empty. Every
constructor **copies** its array, so later changes to the caller's array do not leak in.

```java
RangeToolkit.applyRangeAdds(6, new int[][] {{1, 4, 10}, {3, 6, 5}});  // [0, 10, 10, 15, 5, 5]

var f = new RangeToolkit.Fenwick(new long[] {3, 2, 4, 1, 5});
f.sum(0, 3);          // 9
f.add(3, 4);          // {3, 2, 4, 5, 5}
f.firstReaching(10);  // 3: [0, 2] adds up to 9, [0, 3] to 14
f.firstReaching(9);   // 2

var sparse = new RangeToolkit.SparseMin(new long[] {5, 2, 8, 2, 9, 1});
sparse.min(2, 6);     // 1
var tree = new RangeToolkit.MaxTree(new long[] {5, 2, 8, 2, 9, 1});
tree.argMax(0, 4);    // 2 (the 8)
```

`firstReaching(target)` returns the smallest position `p` such that the values of `[0, p]` add up
to at least `target`: the position where a running total first reaches the target.

## What to write

| Method | Does | Cost |
| --- | --- | --- |
| `applyRangeAdds(n, updates)` | an array of `n` zeros after adding `delta` to every position of `[lo, hi)` for each update `{lo, hi, delta}` | O(n + updates) |
| `Fenwick(values)` | builds the tree | O(n) or O(n log n) |
| `size()`, `get(i)` | the number of values, the value at `i` | O(1) |
| `add(i, delta)`, `set(i, value)` | change one value | O(log n) |
| `sum(lo, hi)` | the sum of `[lo, hi)`, 0 when empty | O(log n) |
| `firstReaching(target)` | see above; -1 if the total is below `target` | O(log n) |
| `SparseMin(values)`, `min(lo, hi)` | the minimum of a range of a fixed array | O(n log n) build, **O(1)** query |
| `MaxTree(values)`, `set(i, value)` | change one value | O(log n) |
| `max(lo, hi)`, `argMax(lo, hi)` | the maximum of a range and its **first** position | O(log n) |

Sums are `long`: two additions of two billion each must not wrap around.

## Errors

- An index outside the array, or a range with `lo < 0`, `hi > n` or `lo > hi`, throws
  `IndexOutOfBoundsException` (also for the ranges inside `applyRangeAdds`).
- `min`, `max` and `argMax` of an empty range, a negative `n` in `applyRangeAdds`, and a
  `firstReaching` target below 1 throw `IllegalArgumentException`.
- `firstReaching` only makes sense when the running total never goes down, so it throws
  `IllegalStateException` while any value is negative.

## How it is graded

The hidden tests compare every method with plain loops on hundreds of small random arrays (with
many ties and negative values), then time 300,000 range adds over 300,000 positions, 400,000
rounds of `add`, `sum` and `firstReaching` on a million values, 2 million `min` queries on a
million values and 500,000 rounds of `set` and `argMax` on half a million values. A loop over the
range fails all of them.

> Tip: `firstReaching` can binary search over `sum(0, p + 1)`, which costs O(log² n). The lesson's
> descent over powers of two does it in O(log n) by reading the Fenwick cells directly.
