# Range Queries: Prefix Sums, Sparse Tables, Fenwick and Segment Trees

A dashboard asks "how many orders between 09:00 and 11:30?", a leaderboard asks "what rank is a
score of 8,120?", a monitoring tool asks "what was the lowest latency in this window?". Each is a
**range query** over an array: combine the values between two positions. A loop answers it in
O(n). That is fine once, and far too slow when the array has a million entries and the questions
arrive thousands of times a second, possibly mixed with updates.

The right structure depends on two questions: **does the array change** between queries, and
**which operation** combines the values.

## Static arrays

**Prefix sums** (from the Fundamentals phase): `prefix[i]` is the sum of the first `i` values,
so the sum over the half-open range `[lo, hi)` is `prefix[hi] - prefix[lo]`. Build O(n), query
O(1). They work for any operation with an inverse (sum, XOR, count), and extend to 2D:

```text
sum of rectangle (r1, c1)..(r2, c2), exclusive ends:
P[r2][c2] - P[r1][c2] - P[r2][c1] + P[r1][c1]
```

A **difference array** is the reverse trick for many *range updates* followed by reads: to add
`x` to `[lo, hi)`, do `diff[lo] += x; diff[hi] -= x`, and a final prefix sum gives every value.

**Minimum and maximum have no inverse**, so prefix sums do not help. A **sparse table** stores,
for every start `i` and every power of two `2^k`, the minimum of `[i, i + 2^k)`:

```java
min[0] = values;                                  // ranges of length 1
for (int k = 1; (1 << k) <= n; k++)
    for (int i = 0; i + (1 << k) <= n; i++)
        min[k][i] = Math.min(min[k - 1][i], min[k - 1][i + (1 << (k - 1))]);
```

A query of `[lo, hi)` takes the largest `k` with `2^k ≤ hi - lo` and combines two ranges that
**overlap**: `min(min[k][lo], min[k][hi - 2^k])`. Build O(n log n), query **O(1)**. Overlap is
only harmless for **idempotent** operations (min, max, gcd, bitwise AND/OR): counting the middle
twice would double a sum.

## Arrays that change: the Fenwick tree

A **Fenwick tree** (binary indexed tree) keeps partial sums so that both a point update and a
prefix sum cost O(log n). It is an array `tree[1..n]` where `tree[i]` holds the sum of the
`i & -i` values ending at position `i`; `i & -i` is the lowest set bit of `i` (the Mastery phase
explains why).

```java
void add(int index, long delta) {          // index is 0-based
    for (int i = index + 1; i <= n; i += i & -i) tree[i] += delta;
}
long prefixSum(int count) {                // sum of the first count values
    long sum = 0;
    for (int i = count; i > 0; i -= i & -i) sum += tree[i];
    return sum;
}
```

Each loop touches at most log₂ n + 1 cells. Two more facts make it useful in practice:

- **Build in O(n)**, not n updates: copy the values to `tree[1..n]`, then for each `i` add
  `tree[i]` to `tree[i + (i & -i)]` if that index exists.
- **Search by prefix in O(log n)**: when every value is non-negative, the prefix sums only grow,
  so "the first position where the running total reaches t" can be found by walking down the
  powers of two instead of binary searching over `prefixSum` (which would be O(log² n)). This is
  how a Fenwick tree of counts answers "what is the k-th smallest score?".

## Any associative operation: the segment tree

A **segment tree** is a complete binary tree over the array: each leaf is one value, each inner
node combines its two children. The iterative version fits in an array of `2n`: leaves at
`[n, 2n)`, the parent of `i` at `i / 2`.

```java
void set(int index, long value) {
    int i = index + n;
    tree[i] = value;
    for (i /= 2; i >= 1; i /= 2) tree[i] = Math.min(tree[2 * i], tree[2 * i + 1]);
}
long min(int lo, int hi) {                 // [lo, hi)
    long best = Long.MAX_VALUE;
    for (lo += n, hi += n; lo < hi; lo /= 2, hi /= 2) {
        if ((lo & 1) == 1) best = Math.min(best, tree[lo++]);
        if ((hi & 1) == 1) best = Math.min(best, tree[--hi]);
    }
    return best;
}
```

Update and query are O(log n), and the operation only has to be **associative** (sum, min, max,
gcd, "the maximum and the position of its first occurrence", matrix product). Unlike the sparse
table, the segment tree supports updates; unlike the Fenwick tree, it does not need an inverse.

**Lazy propagation** adds range updates ("add 5 to every value in `[lo, hi)`") in O(log n): a node
covering a whole updated range stores a pending update and pushes it to its children only when a
later query or update needs to go below it. Use the recursive version with an array of `4n` for
this; it is longer but follows the tree directly.

## Choosing

| Updates | Operation | Structure | Build | Query | Update |
| --- | --- | --- | --- | --- | --- |
| none | sum, XOR, count | prefix sums | O(n) | O(1) | rebuild O(n) |
| many range updates, then reads | sum | difference array | O(n) | read after O(n) | O(1) |
| none | min, max, gcd | sparse table | O(n log n) | O(1) | rebuild |
| point updates | sum (has an inverse) | Fenwick tree | O(n) | O(log n) | O(log n) |
| point updates | any associative op | segment tree | O(n) | O(log n) | O(log n) |
| range updates | sum, min, max | segment tree with lazy propagation | O(n) | O(log n) | O(log n) |

A simpler fallback is **square-root decomposition**: blocks of √n values with a precomputed
summary each, O(√n) per query and update. It is easy to get right and often fast enough.

## Where you meet them

- **Time series**: monitoring systems precompute sums per minute and hour (prefix sums over
  buckets) so dashboards never scan raw points.
- **Ranking**: a Fenwick tree of counts per score answers "how many players scored less than x"
  and "who is k-th" in O(log n) while scores change; it also counts inversions in O(n log n).
- **Lowest common ancestor** in a tree reduces to a range minimum over an Euler tour, answered by
  a sparse table in O(1).
- **Scheduling and geometry**: segment trees over time slots answer "is any room free in this
  interval?" and power sweep-line algorithms over rectangles.

## Pitfalls

- **Half-open ranges** `[lo, hi)` remove most off-by-one errors: the length is `hi - lo`, an
  empty range is `lo == hi`, and two adjacent ranges share no position.
- **Overflow**: a million values of a million each do not fit in an `int`. Sums are `long`.
- **1-based Fenwick**: `i & -i` is 0 for `i = 0`, so an update loop starting at index 0 never
  ends. Shift by one at the boundary of the class, as above.
- **Sparse tables do not update**: one change invalidates O(n log n) cells. If values change, use
  a segment tree.

## Summary

- Static sums: prefix sums. Static min or max: a sparse table, O(1) per query.
- Point updates with sums: a Fenwick tree, O(log n) for both, with an O(log n) search by prefix.
- Point updates with any associative operation: a segment tree; range updates add lazy
  propagation.
- Choose by two questions: does the array change, and does the operation have an inverse or
  tolerate overlap?

## Further reading

- cp-algorithms, "Fenwick Tree": <https://cp-algorithms.com/data_structures/fenwick.html>
- cp-algorithms, "Segment Tree": <https://cp-algorithms.com/data_structures/segment_tree.html>
- cp-algorithms, "Sparse Table": <https://cp-algorithms.com/data_structures/sparse-table.html>
- Peter M. Fenwick, "A New Data Structure for Cumulative Frequency Tables" (1994), the original paper.
- Al.Cash, "Efficient and easy segment trees" (the iterative 2n version): <https://codeforces.com/blog/entry/18051>
