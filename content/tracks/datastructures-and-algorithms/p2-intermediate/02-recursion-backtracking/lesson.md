# Recursion, Divide and Conquer and Backtracking

Recursion is a function that solves a problem by solving smaller copies of it. It is the natural
way to write anything shaped like a tree: file systems, JSON, parse trees, organisation charts,
and every "try all the options" search. It also has a cost model of its own and a hard limit,
the call stack.

## Writing a recursion

Every recursive function needs two things:

1. A **base case** that answers directly.
2. A **recursive case** that reduces the problem to strictly smaller ones and combines their
   answers.

```java
long power(long base, int exp) {          // fast exponentiation: O(log exp) calls
    if (exp == 0) return 1;                // base case
    long half = power(base, exp / 2);      // a problem half the size
    return exp % 2 == 0 ? half * half : half * half * base;
}
```

Think of the recursive call as a **trusted helper**: assume it returns the right answer for the
smaller input, and only check that your combination step and base case are right. That is
mathematical induction, and it is the way to read recursive code without tracing every call.

## What recursion costs

- **Time**: count the calls and the work per call. Draw the **recursion tree**: one node per
  call, children for its recursive calls.
- **Space**: each pending call keeps a **stack frame**. The deepest path of the tree is the stack
  depth.

| Recursion | Calls | Time | Depth |
| --- | --- | --- | --- |
| `factorial(n)` | n | O(n) | n |
| binary search | log n | O(log n) | log n |
| naive `fib(n)` | about 1.6ⁿ | exponential | n |
| merge sort | 2n | O(n log n) | log n |
| all subsets of n | 2ⁿ | O(2ⁿ · n) to list them | n |

### Recurrences and the master theorem

Divide and conquer algorithms have costs of the form `T(n) = a·T(n/b) + f(n)`: `a` subproblems
of size `n/b`, plus `f(n)` work to split and combine. The common cases:

| Recurrence | Example | Result |
| --- | --- | --- |
| T(n) = T(n/2) + O(1) | binary search | O(log n) |
| T(n) = 2T(n/2) + O(1) | tree traversal | O(n) |
| T(n) = 2T(n/2) + O(n) | merge sort, quicksort on average | O(n log n) |
| T(n) = T(n − 1) + O(n) | quicksort with bad pivots, selection sort | O(n²) |
| T(n) = 2T(n − 1) + O(1) | naive Fibonacci, towers of Hanoi | O(2ⁿ) |

The rule of thumb behind the master theorem: compare the work at the top (`f(n)`) with the
number of leaves (`n^(log_b a)`); the bigger one wins, and when they match you multiply by
`log n`.

### The stack limit

A Java thread's stack is typically 512 KB to 1 MB (set with `-Xss` or a `Thread` constructor).
That allows some thousands to tens of thousands of frames, depending on their size. Recursing
along a linked list or a degenerate tree of a million nodes throws `StackOverflowError`. Java
does not eliminate tail calls, so a tail-recursive function still uses a frame per call. The
fixes:

- Recurse only where the depth is logarithmic (balanced trees, divide and conquer).
- Convert to a loop with an **explicit stack** (`ArrayDeque`), as the Heap and Trees kata
  requires for its 200,000-level trees.
- Run deep recursion in a thread with a larger stack, as a last resort.

## Divide and conquer

Split the problem into independent parts, solve them recursively, combine:

- **Merge sort**: split in half, sort both, merge in O(n). Merging also counts **inversions**,
  because when an element from the right half is taken first it jumps over everything left in
  the left half.
- **Quicksort / quickselect**: partition around a pivot; recurse into both sides (sort) or only
  the side that holds the k-th element (select, O(n) on average).
- **Closest pair of points, Karatsuba multiplication, Strassen**: textbook classics that beat the
  obvious bound by clever combining.
- **Fork/join parallelism**: independent halves can run on different cores; Java's
  `ForkJoinPool` and `Arrays.parallelSort` are built on this.

## Backtracking

Many problems ask you to **search** a space of choices: all subsets, all permutations, place 8
queens, fill a Sudoku, find a path through a maze, choose coins that sum to a target. Backtracking
builds a candidate one choice at a time and abandons it as soon as it cannot work:

```java
void backtrack(State state, List<Result> out) {
    if (state.isComplete()) { out.add(state.snapshot()); return; }
    for (Choice c : state.choices()) {
        if (!state.allows(c)) continue;   // prune
        state.apply(c);                    // choose
        backtrack(state, out);             // explore
        state.undo(c);                     // unchoose
    }
}
```

The three classic shapes:

| Problem | Choice at each level | Count |
| --- | --- | --- |
| subsets | include element i or not | 2ⁿ |
| combinations of size k / summing to a target | the next element, from index `start` on (no going back, so no duplicates) | C(n, k) |
| permutations | any unused element (track a `boolean[] used`) | n! |

**Pruning** is what makes backtracking usable: check constraints as early as possible, sort the
choices so you can stop a loop early (if the candidates are sorted and this one is too big, all
later ones are too), choose the most constrained decision first, and skip symmetric choices.
Exact cover solvers, SAT solvers and constraint solvers used in scheduling and configuration are
backtracking with very clever pruning.

When the same subproblem appears again and again in the tree, backtracking becomes **dynamic
programming** with memoisation: that is phase 3.

## Interview checklist for recursion

- State the base case first, out loud.
- Say what the function returns for a smaller input ("the trusted helper").
- Give time from the recursion tree and space from the depth.
- Mention the stack limit when the depth could be n, and offer the iterative version.
- In backtracking, show the undo step and at least one pruning rule.

## Summary

- Recursion = base case + smaller subproblems + combination; read it by induction.
- Time comes from the recursion tree, space from its depth.
- Divide and conquer recurrences: halving with linear work gives O(n log n).
- Java does not optimise tail calls; deep recursion needs an explicit stack.
- Backtracking is choose, explore, unchoose, and pruning decides whether it finishes.

## Further reading

- roadmap.sh, DSA topic "Recursion": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/recursion@4ZAzcwP0oPGZ9Rw042V8-.md>
- roadmap.sh, DSA topic "Backtracking": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/backtracking@mKoC5o__gLSTjAlq8iXkV.md>
- roadmap.sh, DSA topic "Divide and Conquer": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/divide-and-conquer@xchWhsizaKgYuHPC4Tz9H.md>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020), recursion and recurrences: <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- Wikipedia, "Master theorem (analysis of algorithms)": <https://en.wikipedia.org/wiki/Master_theorem_(analysis_of_algorithms)>
- JDK 25 API, `ForkJoinPool` (divide and conquer in parallel): <https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/ForkJoinPool.html>
