# Two Pointers, Sliding Windows and Prefix Sums

A large share of array and string problems have an obvious answer with two nested loops, O(n²),
and a clever answer with one pass, O(n). The clever answers almost always use one of three
techniques from this lesson. The trick is always the same: find a reason why, after looking at a
position, you **never need to look back**.

## Two pointers from opposite ends

On **sorted** data, put one pointer at each end and move them towards each other.

```java
// is there a pair with sum == target in sorted a?
int i = 0, j = a.length - 1;
while (i < j) {
    long sum = (long) a[i] + a[j];
    if (sum == target) return true;
    if (sum < target) i++;      // a[i] is too small for every remaining j
    else j--;                   // a[j] is too large for every remaining i
}
return false;
```

Why is it safe to discard? If `a[i] + a[j]` is too small, then `a[i]` plus any value left of `j`
is even smaller, so `a[i]` can never be part of the answer. Each step eliminates one candidate:
at most n steps. The same pattern checks palindromes, finds the "container with most water",
and solves 3-sum in O(n²) by fixing one element and running two pointers on the rest.

## Two pointers in the same direction

A **fast** pointer reads, a **slow** pointer writes or marks a boundary:

- **Remove in place**: copy `a[read]` to `a[write++]` only when you keep it.
- **Deduplicate a sorted array**: keep `a[read]` when it differs from `a[write - 1]`.
- **Partition** (the core of quicksort, next phase): everything before `write` is small.
- **Linked lists**: fast and slow pointers find the middle or a cycle (previous lesson).

## Sliding windows

A window is a range `[left, right]` that moves across the data.

### Fixed size

For "every window of k values", keep a running aggregate: add the value entering on the right,
remove the one leaving on the left. Sums and counts update in O(1). The window **maximum**
needs the monotonic deque from the previous lesson.

### Variable size

For "the longest/shortest range such that ...", grow the window on the right and shrink it on
the left, keeping an **invariant** true:

```java
// shortest subarray of POSITIVE values with sum >= target
int best = Integer.MAX_VALUE, left = 0;
long sum = 0;
for (int right = 0; right < a.length; right++) {
    sum += a[right];                       // grow
    while (sum >= target) {                // invariant broken in our favour: record and shrink
        best = Math.min(best, right - left + 1);
        sum -= a[left++];
    }
}
```

```java
// longest substring without repeated characters
Map<Character, Integer> last = new HashMap<>();
int best = 0, left = 0;
for (int right = 0; right < s.length(); right++) {
    Integer seen = last.put(s.charAt(right), right);
    if (seen != null && seen >= left) left = seen + 1;   // jump past the repeat
    best = Math.max(best, right - left + 1);
}
```

Both pointers only move forward, so the total work is O(n) even with the inner `while`.

**When a window does not work.** The window relies on **monotonicity**: growing the window makes
the property "more true" (the sum grows, more distinct characters) and shrinking makes it "less
true". With negative numbers, removing a value can *increase* the sum, and the invariant
collapses. Then reach for prefix sums.

## Prefix sums with a hash map

With `prefix[k]` = sum of the first k values, the sum of `a[i..j]` is
`prefix[j + 1] - prefix[i]`. So a subarray ending at j sums to `k` exactly when some earlier
prefix equals `prefix[j + 1] - k`. A hash map from prefix value to how many times it occurred
turns that into one pass, with negatives allowed:

```java
Map<Long, Integer> seen = new HashMap<>(Map.of(0L, 1));
long prefix = 0, count = 0;
for (int v : a) {
    prefix += v;
    count += seen.getOrDefault(prefix - k, 0);
    seen.merge(prefix, 1, Integer::sum);
}
```

Variations: store the **first index** of each prefix instead of a count to find the **longest**
subarray with sum k; use prefix sums **mod k** to find subarrays whose sum is divisible by k;
replace sums by XOR for the XOR versions.

## Choosing a technique

| The problem says | Try |
| --- | --- |
| sorted input, find a pair or triple | opposite-end pointers |
| in place, remove or compact | read and write pointers |
| contiguous range, longest/shortest with a monotonic property | variable sliding window |
| every window of size k | fixed window with a running aggregate (deque for max/min) |
| contiguous range with an exact sum, negatives allowed | prefix sums + hash map |
| many range-sum queries | a prefix-sum array |

## In real systems

- **Rate limiters** use sliding windows over request timestamps (sliding log or sliding window
  counter), the same structure as the Fundamentals checkpoint kata.
- **Monitoring** (Prometheus `rate()`, moving averages, p99 over the last minute) computes
  aggregates over sliding time windows.
- **TCP** uses a sliding window to limit the bytes in flight; **Kafka Streams** and **Flink**
  offer tumbling and sliding windows as first-class operators.
- **Merge joins** in databases walk two sorted inputs with two pointers.

## Summary

- Two pointers from both ends work on sorted data: each step discards a candidate.
- Read/write pointers compact or partition in place.
- A variable window grows on the right, shrinks on the left and needs a monotonic property.
- Both pointers only move forward, so the total is O(n).
- Prefix sums with a hash map handle exact sums and negative values.

## Further reading

- roadmap.sh, DSA topic "Two Pointer Technique": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/two-pointer-technique@itvhUXbe9dr9JN5ga2jYy.md>
- roadmap.sh, DSA topic "Sliding Window Technique": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/sliding-window-technique@gbHVR-ojRxv9I0E5eSlWy.md>
- cp-algorithms, "Minimum stack / Minimum queue" (the monotonic window): <https://cp-algorithms.com/data_structures/stack_queue_modification.html>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020): <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- Wikipedia, "Prefix sum": <https://en.wikipedia.org/wiki/Prefix_sum>
