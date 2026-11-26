# Greedy Algorithms

A greedy algorithm builds a solution one step at a time, always taking the option that looks best
**right now**, and never goes back. When it works it is simple and fast, usually a sort plus one
pass. The catch is that it is often wrong, and an interview answer (or production code) that uses
greedy without a reason is a gamble. This lesson is about telling the two cases apart.

## When greedy is right

Two properties make a greedy algorithm optimal:

- **Greedy choice property**: some optimal solution starts with the greedy choice.
- **Optimal substructure**: after the choice, what remains is a smaller instance of the same
  problem.

You show the first one with an **exchange argument**: take any optimal solution; if it does not
start with the greedy choice, swap that choice in and show nothing gets worse. You disprove a
greedy rule with a **counterexample**, often with three or four items.

## The interval family

Most greedy interview problems are about intervals, and the whole skill is choosing the **sort
key**.

### Activity selection: the most non-overlapping intervals

Sort by **end** time; take an interval whenever it starts at or after the end of the last one
taken.

```java
Arrays.sort(talks, Comparator.comparingInt(t -> t[1]));   // by end
int count = 0, lastEnd = Integer.MIN_VALUE;
for (int[] t : talks) {
    if (t[0] >= lastEnd) { count++; lastEnd = t[1]; }
}
```

**Why ending earliest is safe**: let O be an optimal schedule whose first talk ends later than
the earliest-ending talk g. Replacing O's first talk with g cannot create an overlap (g ends even
earlier), and the count stays the same. Repeat for the rest.

Wrong keys and their counterexamples:

| Rule | Counterexample |
| --- | --- |
| shortest first | `[1, 5)`, `[6, 10)`, and the short `[4, 7)` that blocks both |
| earliest start first | one long `[0, 100)` that blocks ten short ones |
| fewest overlaps first | known counterexamples exist with about a dozen intervals |

### Other interval problems

| Problem | Sort by | Then |
| --- | --- | --- |
| merge overlapping intervals | start | extend the current interval while the next starts inside it |
| minimum rooms / platforms / servers | start | a min-heap of end times; the answer is its largest size (or sweep sorted starts and ends) |
| minimum arrows to burst balloons | end | shoot at the end of the first, skip all it hits |
| remove fewest intervals to avoid overlap | end | n − (activity selection count) |
| insert an interval into a sorted list | (already sorted) | copy before, merge the overlap, copy after |

## Other classic greedy algorithms

- **Huffman coding**: repeatedly merge the two least frequent nodes (a min-heap) into a tree;
  frequent symbols get short codes. It is part of DEFLATE (gzip, zip, PNG) and JPEG.
- **Dijkstra** and **Prim**: always settle the closest vertex or the lightest crossing edge.
- **Kruskal**: always add the lightest edge that does not close a cycle.
- **Fractional knapsack**: take items by value per weight; split the last one. (The 0/1 version
  needs DP.)
- **Jump game / gas station**: keep the farthest reachable index or a running balance in one
  pass.
- **Task scheduling with cooldowns**: always run the task with the most remaining copies.
- **Change-making** with "canonical" coin systems (euro, dollar). For arbitrary coins, use DP.

## Greedy or dynamic programming?

| Signal | Lean towards |
| --- | --- |
| you can prove an exchange argument | greedy |
| a small counterexample breaks the obvious rule | DP |
| items are indivisible and have weights and values | DP (knapsack) |
| "minimum number of ..." over intervals with no values | greedy |
| intervals with values ("maximum total value") | DP with binary search (weighted interval scheduling) |
| the problem asks for all solutions or a count | DP or backtracking |

A good interview habit: propose the greedy rule, then immediately try to break it with a tiny
example. If you cannot, say why it is safe.

## Greedy in real systems

- **Caching**: evicting the least recently used item (LRU) is a greedy heuristic; Belady's
  optimal policy (evict the item used farthest in the future) is greedy too, but needs the
  future.
- **Schedulers** pick the shortest job or the earliest deadline; Kubernetes scores nodes and
  places each pod greedily on the best one.
- **Load balancers** send each request to the least loaded backend.
- **Compression**: Huffman in gzip; LZ77's longest-match search is greedy (with lazy matching).
- **Approximation**: for hard problems (set cover, bin packing), greedy gives provably good but
  not optimal answers, which is often what production needs.

## Summary

- Greedy takes the locally best step and never reconsiders.
- It is optimal only with the greedy choice property: prove it with an exchange argument,
  disprove it with a counterexample.
- Interval problems are solved by sorting on the right key (usually the end).
- Huffman, Dijkstra, Prim and Kruskal are greedy algorithms with proofs.
- When in doubt, DP is the safe choice; greedy is often a good approximation.

## Further reading

- roadmap.sh, DSA topic "Greedy Algorithms": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/greedy-algorithms@qUlqyTjk3XKF9DEm6Lsgb.md>
- roadmap.sh, DSA topic "Merge Intervals": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/merge-intervals@S2mBvlFTd673XcXxisD5P.md>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Data Compression" (Huffman): <https://algs4.cs.princeton.edu/55compression/>
- MIT OCW 6.006 Introduction to Algorithms (Spring 2020): <https://ocw.mit.edu/courses/6-006-introduction-to-algorithms-spring-2020/>
- Wikipedia, "Interval scheduling": <https://en.wikipedia.org/wiki/Interval_scheduling>
