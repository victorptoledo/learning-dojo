# Dynamic Programming

Dynamic programming (DP) is recursion that **remembers**. When a brute-force search solves the
same subproblems again and again, storing each answer the first time turns exponential time into
polynomial time. DP has a reputation for being hard, mostly because people start from the final
table. Start from the recursion instead, and the table follows.

## When DP applies

Two properties:

- **Overlapping subproblems**: the recursion tree contains the same calls many times.
  `fib(5)` calls `fib(3)` twice and `fib(2)` three times.
- **Optimal substructure**: the best answer to the problem is built from best answers to
  subproblems. The shortest route from A to C through B uses the shortest route from A to B.

If subproblems do not overlap (merge sort), it is plain divide and conquer. If the best answer
cannot be assembled from best sub-answers (longest simple path in a general graph), DP does not
apply directly.

## The routine: recursion → memo → table

Take "how many ways to climb n stairs taking 1 or 2 steps":

```java
// 1. brute force: the arguments are the STATE, the recursion is the RECURRENCE
long ways(int n) { return n <= 1 ? 1 : ways(n - 1) + ways(n - 2); }   // O(1.6ⁿ)

// 2. memoisation (top-down): same recursion, cache by state
long ways(int n, long[] memo) {
    if (n <= 1) return 1;
    if (memo[n] != 0) return memo[n];
    return memo[n] = ways(n - 1, memo) + ways(n - 2, memo);         // O(n)
}

// 3. tabulation (bottom-up): fill states in an order where inputs are ready
long[] dp = new long[n + 1];
dp[0] = dp[1] = 1;
for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];          // O(n)

// 4. rolling memory: only the last two values are needed
long a = 1, b = 1;
for (int i = 2; i <= n; i++) { long c = a + b; a = b; b = c; }       // O(1) memory
```

For every DP, write down:

1. **State**: what does `dp[...]` mean, in one sentence? ("dp[i][c] = best value using the
   first i items with capacity c".)
2. **Recurrence**: how does a state follow from smaller ones?
3. **Base cases**: the states you know directly.
4. **Order**: an order in which every state's inputs come first.
5. **Answer**: which state (or the best over which states) is the result.

**Cost = number of states × work per state.** That one formula estimates every DP.

### Memoisation or tabulation?

| | Top-down (memo) | Bottom-up (table) |
| --- | --- | --- |
| Writing it | add a cache to the recursion | find a fill order |
| Computes | only the states it reaches | every state |
| Stack | recursion depth (can overflow) | none |
| Memory tricks | hard | rolling rows, in-place updates |
| Typical use | interviews, sparse state spaces | production, big inputs |

In Java, memoise with an array when states are small integers, and with a `HashMap` keyed by a
record when they are not.

## The classic families

| Family | State | Recurrence (sketch) | Cost |
| --- | --- | --- | --- |
| 1D sequence (stairs, house robber) | dp[i] = best for the first i | from dp[i-1], dp[i-2] | O(n) |
| grid paths | dp[r][c] = ways to reach (r, c) | dp[r-1][c] + dp[r][c-1] | O(R·C) |
| 0/1 knapsack | dp[c] after the first i items | max(dp[c], dp[c-w]+v), c downwards | O(n·C) |
| unbounded knapsack, coin change | dp[a] = fewest coins for amount a | 1 + min over coins of dp[a-coin], a upwards | O(A·k) |
| longest increasing subsequence | tails[len] = smallest tail | binary search for the slot | O(n log n) |
| edit distance, LCS | dp[i][j] over prefixes of both strings | from (i-1, j-1), (i-1, j), (i, j-1) | O(n·m) |
| word break | can[i] = first i chars split | any can[j] with s[j..i) a word | O(n·L) |
| intervals (matrix chain, burst balloons) | dp[i][j] over a range | split point k between i and j | O(n³) |
| DP on trees | dp[node] from its children | post-order | O(n) |
| bitmask DP (travelling salesman) | dp[set][last] | add one element to the set | O(2ⁿ·n²) |

Knapsack shows why the **loop direction** matters: with one row, iterating the capacity from
high to low uses each item once (0/1); from low to high, an item can be reused (unbounded).

Edit distance in one picture, turning "kitten" into "sitting":

```text
d[i][j] = distance between the first i chars of a and the first j chars of b
d[i][j] = d[i-1][j-1]                         if a[i-1] == b[j-1]
        = 1 + min(d[i-1][j-1],  substitute
                  d[i-1][j],    delete from a
                  d[i][j-1])    insert into a
d[0][j] = j, d[i][0] = i                       answer: d[6][7] = 3
```

## Getting the solution, not only its value

Keep the full table (or a "choice" array) and walk back from the answer: at each state, ask
which option produced its value. For knapsack, item i was taken if `dp[i][c] != dp[i-1][c]`.
Rolling rows save memory but lose this trail; Hirschberg's algorithm recovers an alignment in
linear memory when it matters.

## DP or greedy?

A greedy algorithm takes the locally best option and never reconsiders. It is right only for
problems with a proof (next phase). For coin systems like {1, 3, 4}, greedy pays 6 as 4 + 1 + 1
while DP finds 3 + 3. When you cannot prove a greedy choice, DP is the safe default.

## In real systems

- **diff** and **git** compute a minimal edit script between files (Myers' algorithm, an edit
  distance on lines).
- **Spell checkers and fuzzy search** (Elasticsearch `fuzziness`) use edit distance.
- **Bioinformatics** aligns DNA with Needleman-Wunsch and Smith-Waterman, both edit-distance DPs.
- **Query optimisers** in PostgreSQL choose join orders with a DP over subsets of tables (and
  switch to a genetic algorithm when there are too many).
- **Text layout**: TeX breaks paragraphs into lines with a DP that minimises "badness".
- **Speech and error-correcting codes** decode with Viterbi, a DP over states and time.

## Summary

- DP = recursion + remembering, for overlapping subproblems with optimal substructure.
- Write state, recurrence, base cases, order and answer; cost = states × work per state.
- Memoise to get it right, tabulate to get it fast and small; roll rows to save memory.
- Learn the families: sequences, grids, knapsack, strings, intervals, trees, bitmasks.
- Keep the choices if you need the solution itself, not just its value.

## Further reading

- cp-algorithms, "Introduction to Dynamic Programming": <https://cp-algorithms.com/dynamic_programming/intro-to-dp.html>
- cp-algorithms, "Longest increasing subsequence": <https://cp-algorithms.com/dynamic_programming/longest_increasing_subsequence.html>
- cp-algorithms, "Knapsack Problem": <https://cp-algorithms.com/dynamic_programming/knapsack.html>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020), dynamic programming lectures: <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- Wikipedia, "Levenshtein distance": <https://en.wikipedia.org/wiki/Levenshtein_distance>
- roadmap.sh, DSA topic "Dynamic Programming": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/dynamic-programming@jKAZNybU9cxufNBdFsqsP.md>
