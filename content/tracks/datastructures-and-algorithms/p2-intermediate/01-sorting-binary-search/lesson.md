# Sorting and Binary Search

Sorting is the most studied problem in computing, and in practice you will rarely write a sort:
`List.sort` is excellent. What you do need is to know **what the library does**, what it costs,
when **not** to sort at all, and how to write the one algorithm that sorted data enables:
**binary search**, which is famously easy to get almost right.

## The algorithms worth knowing

| Algorithm | Time (worst) | Time (typical) | Extra memory | Stable | Idea |
| --- | --- | --- | --- | --- | --- |
| Insertion sort | O(n²) | O(n) on nearly sorted | O(1) | yes | insert each item into the sorted prefix |
| Merge sort | O(n log n) | O(n log n) | O(n) | yes | sort halves, merge them |
| Quicksort | O(n²) | O(n log n) | O(log n) stack | no | partition around a pivot, recurse |
| Heapsort | O(n log n) | O(n log n) | O(1) | no | build a heap, pop the max repeatedly |
| Counting / radix sort | O(n + k) | O(n + k) | O(n + k) | yes | count keys from a small range, no comparisons |

- **Insertion sort** is the fastest for small or nearly sorted arrays, which is why hybrid sorts
  use it for short runs.
- **Merge sort** is predictable and stable, and it is the basis of **external sorting** (sort
  chunks that fit in memory, then merge files), used by databases for big `ORDER BY`s.
- **Quicksort** is usually the fastest in memory: in-place partitioning is cache friendly. Its
  worst case comes from bad pivots (a first-element pivot on sorted data) and from many equal
  keys with a two-way partition; random pivots and **three-way partitioning** fix both, as the
  Search Toolkit kata will show you.
- **Heapsort** gives O(n log n) in O(1) memory but has poor locality; introsort falls back to it
  when quicksort recurses too deep.

### The n log n wall, and how to go around it

Any sort that only **compares** elements needs Ω(n log n) comparisons in the worst case: there
are n! possible orderings, and each comparison can at best halve them. Counting sort and radix
sort escape the bound by **not comparing**: they use the keys as array indexes. They win when
keys are small integers (ages, scores, bytes) or fixed-width (IP addresses, 64-bit ids with
radix sort).

### Stability

A **stable** sort keeps equal elements in their original order. It lets you sort by a secondary
key, then by the primary key, and keep both orders, and it keeps results predictable for users.
Merge sort, TimSort, insertion and counting sort are stable; quicksort and heapsort are not.

## What the JDK does

| Call | Algorithm | Notes |
| --- | --- | --- |
| `Arrays.sort(int[])` and other primitives | dual-pivot quicksort | not stable (does not matter for primitives) |
| `Arrays.sort(Object[])`, `List.sort`, `Collections.sort` | TimSort | stable; O(n) on already sorted input; exploits existing runs |
| `Arrays.parallelSort` | parallel merge on the ForkJoin pool | worth it for millions of elements |
| `stream.sorted()` | collects, then TimSort | needs all elements in memory |

**TimSort** finds already-ordered runs in the data, extends short ones with insertion sort and
merges them carefully. Real data is often partly sorted (log lines, appended records), and
TimSort is linear on it. Python, Android and V8 use it or a close relative too.

### Comparators that do not lie

- Never write `(a, b) -> a.x - b.x`: the subtraction overflows for large values. Use
  `Integer.compare`, `Comparator.comparingInt(T::x)`.
- Chain keys: `Comparator.comparing(Order::customer).thenComparing(Order::date).reversed()`.
- A comparator must be **consistent** (transitive, antisymmetric). TimSort detects some broken
  comparators and throws "Comparison method violates its general contract!".
- `nullsFirst` / `nullsLast` handle missing values explicitly.

### When not to sort

- **Top k of n**: a heap of size k is O(n log k), next lesson but one.
- **The k-th element or the median**: quickselect is O(n) on average.
- **Membership or deduplication**: a hash set is O(n) expected.
- **Data that changes all the time**: keep it in a `TreeMap` or a heap instead of re-sorting.
- **Only a few distinct keys**: counting.

## Binary search

On a sorted array, compare the target with the middle element and discard half. O(log n): 20
steps for a million elements, 30 for a billion. The difficulty is the boundaries. Write the
**invariant** first and make every branch keep it:

```java
// lowerBound: the first index i with a[i] >= target (a.length if none)
// invariant: the answer is in [lo, hi]
int lo = 0, hi = a.length;
while (lo < hi) {
    int mid = (lo + hi) >>> 1;          // no overflow, unlike (lo + hi) / 2
    if (a[mid] < target) lo = mid + 1;  // mid is too small: the answer is after it
    else hi = mid;                      // mid might be the answer: keep it
}
return lo;
```

From `lowerBound` you get everything else: "is it present" (`lo < n && a[lo] == target`),
`upperBound` (use `<=`), "how many equal" (`upperBound - lowerBound`), "insertion point".
`Arrays.binarySearch` returns `-(insertionPoint) - 1` when absent, and does not promise which of
several equal elements it finds.

### Binary search on the answer

Binary search does not need an array. It needs a **monotonic predicate**: false, false, ...,
true, true over some range. Then search for the first true.

```text
"smallest capacity that ships all packages in D days"
  can(c) = greedy simulation in O(n): is the number of days with capacity c ≤ D?
  can is false for small c and true from some c on
  search c in [max(weights), sum(weights)]  →  O(n log(sum))
```

The same shape solves "minimum time to finish", "maximum minimum distance", "smallest divisor
such that ...", `git bisect` (the first bad commit) and finding a square root. When the answer is
a real number, iterate a fixed number of times (say 100) instead of comparing with an epsilon.

## Searching in real systems

- **B-trees** in every database index are binary search over pages, with a high branching factor
  so that a lookup touches 3 or 4 pages for millions of rows.
- **LSM storage** (RocksDB, Cassandra) binary-searches sorted string tables (SSTables) and their
  sparse indexes.
- **`git bisect`** binary-searches history for the commit that broke a test.
- Sorted arrays plus binary search beat hash maps when the data is static and you also need
  ranges: they use less memory and keep order.

## Summary

- Merge sort is stable and predictable; quicksort is fast in memory but needs good pivots;
  counting sort is linear for small key ranges.
- Comparison sorts cannot beat n log n; non-comparison sorts can.
- Java sorts primitives with dual-pivot quicksort and objects with stable TimSort.
- Do not sort when you only need the top k, the k-th element or membership.
- Write binary search from an invariant, with an overflow-safe midpoint, and use it on any
  monotonic predicate.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Mergesort": <https://algs4.cs.princeton.edu/22mergesort/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Quicksort" (including three-way partitioning): <https://algs4.cs.princeton.edu/23quicksort/>
- cp-algorithms, "Binary search": <https://cp-algorithms.com/num_methods/binary_search.html>
- JDK 25 API, `Arrays` (sort, parallelSort, binarySearch): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Arrays.html>
- Joshua Bloch, "Extra, Extra - Read All About It: Nearly All Binary Searches and Mergesorts are Broken" (Google Research blog, 2006): <https://research.google/blog/extra-extra-read-all-about-it-nearly-all-binary-searches-and-mergesorts-are-broken/>
- roadmap.sh, DSA topic "Binary Search": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/binary-search@Wt74MRKaZcs8lYvfqjffT.md>
- roadmap.sh, DSA topic "Sorting Algorithms": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/sorting-algorithms@WzFkdkcbL8ea9DBkjfSwa.md>
