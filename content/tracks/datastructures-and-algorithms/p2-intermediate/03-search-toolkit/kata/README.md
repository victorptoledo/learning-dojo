# Search Toolkit

Six methods of `Search` that practise binary search (on arrays and on the answer), partitioning,
merge sort and backtracking. Do not modify the arrays you receive. The hidden tests use large
inputs under time limits, and some inputs are chosen to break the naive version of each
algorithm.

## `int lowerBound(int[] sorted, int target)`

The first index `i` with `sorted[i] >= target`, or `sorted.length` if there is none. O(log n).

```text
lowerBound([1, 3, 3, 3, 8], 3) → 1     lowerBound([1, 3, 8], 4) → 2     lowerBound([1, 3, 8], 9) → 3
```

## `int searchRotated(int[] rotated, int target)`

`rotated` is an ascending array of **distinct** values rotated at an unknown point, such as
`[30, 40, 50, 10, 20]`. Return the index of `target`, or `-1`. O(log n): at every step one half
of the range is sorted, and you can tell which.

## `int minShipCapacity(int[] weights, int days)`

Packages must leave in order. Each day a ship takes the next packages as long as their total
weight fits its capacity. Return the **smallest capacity** that ships everything within `days`
days. `days < 1` throws `IllegalArgumentException`; weights are positive.

```text
weights [3, 2, 2, 4, 1, 4], days 3 → 6      ([3, 2] [2, 4] [1, 4])
```

This is **binary search on the answer**: "can capacity c do it in `days`?" is false, false, ...,
true, true as c grows, so search for the first true between `max(weights)` and `sum(weights)`.

## `int kthSmallest(int[] values, int k)`

The k-th smallest value (`k` from 1), for example the median. `k` outside `1..length` throws
`IllegalArgumentException`. Use **quickselect** (average O(n)) on a copy: partition around a
**random** pivot into less / equal / greater and continue in one part. The hidden tests include
sorted arrays (a first-element pivot becomes O(n²)) and arrays of one repeated value (a two-way
partition becomes O(n²)).

## `long countInversions(int[] values)`

The number of pairs `i < j` with `values[i] > values[j]`: how far the array is from sorted.
O(n log n) with **merge sort**: when the merge takes an element from the right half, it jumps
over every element still waiting in the left half.

```text
[2, 4, 1, 3, 5] → 3     (2>1, 4>1, 4>3)
```

## `List<List<Integer>> combinationSum(int[] candidates, int target)`

`candidates` are distinct positive values, each usable any number of times. Return every
combination that sums to `target`, each in non-decreasing order, the whole list in
lexicographic order. **Backtracking**: choose a value, recurse with the rest of the target, undo.

```text
candidates [2, 3, 5], target 8 → [[2, 2, 2, 2], [2, 3, 3], [3, 5]]
```

> Tip: sort a copy of the candidates and stop the loop as soon as a candidate is larger than
> what is left of the target; that one test prunes most of the search tree.
