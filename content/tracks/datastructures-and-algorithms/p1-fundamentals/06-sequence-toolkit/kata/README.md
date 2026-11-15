# Sequence Toolkit

Implement the six static methods of `Sequences`. Each must run in **O(n)** (amortised): the
hidden tests use inputs of 200,000 elements under a time limit that a nested loop cannot meet.

## `int[] pairWithSum(int[] sorted, int target)`

`sorted` is in non-decreasing order. Return the indexes `{i, j}` with `i < j` and
`sorted[i] + sorted[j] == target`, choosing the **smallest `i`** that has a partner and, for that
`i`, the **largest `j`**. Return an empty array when there is no pair. Sums may exceed `int`.

```text
pairWithSum([1, 2, 4, 4, 7, 11], 8)  →  [0, 4]   (1 + 7; 4 + 4 also sums to 8 but has a larger i)
```

Two pointers starting at both ends find exactly this pair: they meet the outermost pair first.

## `int longestUniqueRun(String text)`

The length of the longest substring with no repeated character. `""` gives 0.

```text
"abcabcbb" → 3 ("abc")      "pwwkew" → 3 ("wke")      "aaaa" → 1
```

## `int[] nextGreater(int[] values)`

For every index, the first value **to its right** that is strictly greater, or `-1`.

```text
[2, 1, 2, 4, 3] → [4, 2, 4, -1, -1]
```

## `boolean balanced(String text)`

Whether the brackets `()`, `[]` and `{}` in the text are balanced and properly nested. Every
other character is ignored.

```text
"f(a[i]) { return; }" → true      "([)]" → false      "((" → false      "" → true
```

## `long countSubarraysWithSum(int[] values, long k)`

How many contiguous, non-empty subarrays sum to exactly `k`. Values can be negative or zero.

```text
[1, 1, 1], k = 2 → 2      [3, -3, 3, -3], k = 0 → 4
```

## `int shortestWindowWithSum(int[] positives, long atLeast)`

All values are positive. The length of the shortest contiguous subarray whose sum is
**at least** `atLeast`, or `0` if none.

```text
[2, 3, 1, 2, 4, 3], atLeast = 7 → 2   ([4, 3])
```

> Tip: `java.util.ArrayDeque` is the stack to use in Java (`push`, `pop`, `peek`); the old
> `java.util.Stack` is synchronized and extends `Vector`.
