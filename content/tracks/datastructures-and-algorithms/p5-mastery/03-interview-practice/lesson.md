# Solving Problems Out Loud

A coding interview does not only measure whether you reach the optimal solution. It measures
**how you get there**: whether you clarify before you code, reason about costs, communicate,
test your own work and use hints. A candidate who narrates a clear process and lands a good
solution usually beats one who silently writes the optimal answer with a bug. This lesson gives
you a process to practise until it is automatic.

## The process

### 1. Understand and clarify (2–5 minutes)

Restate the problem in your own words, then ask about:

- **Inputs**: types, sizes (10? 10⁵? 10⁹?), ranges, sorted or not, duplicates, negatives,
  empty input, `null`.
- **Outputs**: one answer or all, any valid answer or a specific one, the format, what to return
  when there is none.
- **Constraints**: time and memory limits, streaming or all in memory, can the input be modified.

The size tells you the target complexity:

| n up to | Aim for |
| --- | --- |
| 10–12 | O(n!) or O(2ⁿ · n): backtracking, bitmasks |
| 20–25 | O(2ⁿ): subsets, bitmask DP |
| 500 | O(n³) |
| 5,000 | O(n²) |
| 10⁵–10⁶ | O(n log n) or O(n) |
| 10⁹ and beyond | O(log n) or O(1): math, binary search on the answer |

### 2. Work examples by hand

Write one normal example and solve it **by hand**, slowly. Then think of an edge case. Doing it
by hand often reveals the algorithm, and the examples become your test cases later.

### 3. State a brute force

Say the simplest correct solution and its cost: "Checking every pair is O(n²), which is 10¹⁰
for n = 10⁵: too slow, but it is correct." It shows you understand the problem and gives you a
fallback.

### 4. Find the bottleneck and optimise

Ask: **what work does the brute force repeat?** Then reach for the tool that removes it. The
signals:

| The problem has | Try |
| --- | --- |
| sorted input, pairs or triples | two pointers, binary search |
| contiguous subarray or substring | sliding window, prefix sums |
| "have I seen this?", counting, grouping | hash map / set |
| matching, nesting, "next greater" | stack, monotonic stack |
| top k, k-th, median, merge k | heap, quickselect |
| a monotonic yes/no over a range of answers | binary search on the answer |
| all combinations, permutations, placements | backtracking with pruning |
| overlapping subproblems, "number of ways", "minimum cost" | dynamic programming |
| dependencies, ordering | graph, topological sort |
| shortest steps | BFS (unweighted), Dijkstra (weighted) |
| "are these connected" with merges | union-find |
| prefixes of strings | trie |
| intervals | sort by start or end, sweep line, heap of ends |
| "each appears twice except one", subsets of a small set | bits |

Before coding, say the plan and its complexity, and check that the interviewer agrees.

### 5. Code it cleanly

- Use meaningful names (`left`, `right`, `windowCount`, not `i2`, `tmp`).
- Extract helpers (`isValid`, `neighbours`, `buildGraph`) and say "I'll write this helper next".
- Handle edge cases at the top, explicitly.
- Keep talking, but briefly: what this block does, not every keystroke.
- In Java, reach for the right library pieces: `ArrayDeque`, `HashMap.merge`,
  `PriorityQueue` with a comparator, `Arrays.sort`, records for small value types.

### 6. Test by hand

Trace your code on your example, then on edge cases: empty, one element, all equal, negative
values, maximum sizes (overflow?), the answer at the very start or end. When you find a bug, say
so calmly, fix it and retrace the part that changed.

### 7. Analyse and discuss

State time and memory and **why** ("each element is pushed and popped once, so O(n)"). Mention
trade-offs and what you would do differently in production: streaming input, concurrency,
memory limits, readability.

## Communicating well

- **Think aloud**, including dead ends: "a heap would work, but sorting once is simpler here
  because the data is static".
- **Check in** at the transitions: after clarifying, before coding, after testing.
- **Take hints** as collaboration: acknowledge, connect, continue.
- **Manage time**: if you are stuck for more than a few minutes, code the brute force and
  optimise afterwards.
- **Be honest**: "I don't remember the exact API, I'll assume `pollFirst` exists" is fine.

## Practising

- Practise **out loud**, with a timer, on a plain editor without autocomplete, as in the
  interview.
- Cover the **patterns**, not hundreds of problems: two or three problems per row of the table
  above, then mix them so you must recognise the pattern yourself.
- Review each attempt: where did you lose time, which signal did you miss?
- Use the dojo: the katas of this track, the mock interview (press `I` on the track) and the
  placement test are built for exactly this.

## Summary

- Clarify, examples, brute force, bottleneck, plan, code, test, analyse.
- Input size tells you the target complexity.
- Problem signals map to techniques; say the mapping out loud.
- Clean names, helpers and explicit edge cases make code easy to follow.
- Test by tracing, fix calmly, state the costs and the trade-offs.

## Further reading

- Tech Interview Handbook, "Coding interview techniques": <https://www.techinterviewhandbook.org/coding-interview-techniques/>
- Tech Interview Handbook, "Algorithms study cheatsheet": <https://www.techinterviewhandbook.org/algorithms/study-cheatsheet/>
- roadmap.sh, DSA topic "Problem Solving Techniques": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/problem-solving-techniques@Y1Uz6XRbR1YqFtO4akiPA.md>
- roadmap.sh, DSA topic "Brute Force": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/brute-force@5T2y6Iqi-I_zOtLkeSPzY.md>
- George Pólya, "How to Solve It" (summary of the method): <https://en.wikipedia.org/wiki/How_to_Solve_It>
