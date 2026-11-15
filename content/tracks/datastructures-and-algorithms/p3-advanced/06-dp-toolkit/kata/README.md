# DP Toolkit

Six classic dynamic programming problems in `Dp`. For each one, find the **state** (what a
subproblem is), the **recurrence** (how a state follows from smaller ones) and the **base case**;
then write it top-down with memoisation or bottom-up with a table. The hidden tests use inputs
where plain recursion takes longer than the age of the universe, and one where an O(n²) table is
too slow.

## `long gridPaths(boolean[][] blocked)`

The number of paths from the top-left to the bottom-right cell moving only **right or down**,
avoiding blocked cells, **modulo 1,000,000,007**. A blocked start or end gives 0.

State: `paths[r][c]` = ways to reach (r, c) = `paths[r-1][c] + paths[r][c-1]`.

## `int editDistance(String a, String b)`

The fewest single-character insertions, deletions and substitutions that turn `a` into `b`
(Levenshtein distance). `"kitten"` → `"sitting"` is 3.

State: `d[i][j]` = distance between the first `i` characters of `a` and the first `j` of `b`.
You only ever need the previous row, so O(min(n, m)) memory is enough.

## `int longestIncreasingSubsequence(int[] values)`

The length of the longest **strictly** increasing subsequence (not necessarily contiguous).
`[10, 9, 2, 5, 3, 7, 101, 18]` → 4 (`2, 3, 7, 18`).

The O(n²) table (`lis[i]` = longest ending at i) is correct but too slow for the 200,000 values of
the hidden tests. Use the O(n log n) version: keep `tails[len]` = the smallest possible last value
of an increasing subsequence of length `len + 1`; each value replaces the first tail that is
`>=` it (a lower bound by binary search) or extends the array.

## `long knapsack(int[] weights, int[] values, int capacity)`

Each item can be taken **once**. The largest total value with total weight `<= capacity`.

State: `best[c]` = best value with capacity c using the items seen so far; for each item, update
`c` from high to low so the item is not used twice.

## `int minCoins(int[] coins, int amount)`

The fewest coins (each denomination unlimited) that sum to `amount`, or `-1` if impossible;
`0` for amount 0. Greedy is wrong here: with coins `{1, 3, 4}` and amount 6, greedy takes
`4 + 1 + 1`, but `3 + 3` is better.

## `boolean wordBreak(String text, Set<String> words)`

Whether `text` can be split into a sequence of dictionary words (each usable many times).
`"applepenapple"` with `{apple, pen}` is true. The empty text is true.

State: `can[i]` = whether the first `i` characters can be split.

All inputs are valid: non-negative weights and values, positive coins, a capacity and an amount
`>= 0`.
