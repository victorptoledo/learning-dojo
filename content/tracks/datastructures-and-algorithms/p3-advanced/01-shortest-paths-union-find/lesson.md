# Shortest Paths, Union-Find and Spanning Trees

BFS finds the fewest edges. As soon as edges have **weights** (minutes, kilometres, euros,
milliseconds) you need **Dijkstra**, and a couple of relatives for special cases. This lesson
also adds **union-find**, the structure that answers "are these two connected?" while
connections keep being added, and uses it to build **minimum spanning trees**.

## Shortest paths: which algorithm?

| Graph | Algorithm | Cost |
| --- | --- | --- |
| unweighted | BFS | O(V + E) |
| weights 0 or 1 | 0-1 BFS with a deque | O(V + E) |
| non-negative weights | Dijkstra with a binary heap | O((V + E) log V) |
| a good distance estimate to the goal | A* | often far less than Dijkstra |
| negative weights | Bellman-Ford (detects negative cycles) | O(V · E) |
| a DAG, any weights | relax in topological order | O(V + E) |
| all pairs, small V | Floyd-Warshall | O(V³) |

## Dijkstra

Keep `dist[v]`, the best distance found so far, and a priority queue of `(dist, vertex)`.
Repeatedly take the closest unsettled vertex and **relax** its edges:

```java
dist[source] = 0;
queue.add(new long[] {0, source});
while (!queue.isEmpty()) {
    long[] top = queue.poll();
    int v = (int) top[1];
    if (top[0] > dist[v]) continue;                  // outdated entry: skip it
    for (Edge e : graph.get(v)) {
        long candidate = dist[v] + e.weight();
        if (candidate < dist[e.to()]) {               // relax
            dist[e.to()] = candidate;
            parent[e.to()] = v;                       // to rebuild the path
            queue.add(new long[] {candidate, e.to()});
        }
    }
}
```

**Why it is correct**: with non-negative weights, the closest unsettled vertex cannot be reached
more cheaply through vertices that are farther away. So when it leaves the queue, its distance is
final. A **negative** edge breaks exactly that argument.

**Lazy deletion**: Java's `PriorityQueue` has no "decrease key", and `remove(Object)` is O(n).
Instead, push a new entry whenever a distance improves and skip entries that are worse than
`dist[v]` when they come out. The queue may hold up to E entries; the cost stays
O(E log E) = O(E log V).

**Paths**: keep `parent[v]` on every improvement and walk it back from the target.

### Relatives

- **0-1 BFS**: when weights are only 0 and 1, a deque replaces the heap: push to the front for a
  0-edge, to the back for a 1-edge. Grids where some moves are free use it.
- **A\***: Dijkstra ordered by `dist[v] + h(v)`, where `h` estimates the remaining distance and
  never overestimates it (straight-line distance on a map). It explores towards the goal, which
  is why games and route planners use it.
- **Bellman-Ford**: relax every edge V − 1 times. Slow, but it handles negative weights, and a
  further improvement in round V reveals a **negative cycle** (an arbitrage loop in currency
  rates, for example).
- **DAGs**: relax vertices in topological order; this also gives **longest** paths, which are
  hard in general graphs.

## Union-find (disjoint set union)

Union-find keeps a partition of elements into sets and supports two operations: `find(x)`, the
representative of x's set, and `union(a, b)`, merge two sets. It is stored as a forest: each
element points to a parent, and roots represent their sets.

```text
parent: 0→0  1→0  2→0  3→3  4→3        sets {0, 1, 2} and {3, 4}
union(2, 4): root(2) = 0, root(4) = 3 → attach the smaller tree under the larger
```

Two optimisations make it extremely fast:

- **Union by size (or rank)**: attach the smaller tree under the root of the larger, so trees stay
  O(log n) deep.
- **Path compression**: after `find(x)` walks to the root, point every node on the path straight
  at the root.

Together, a sequence of m operations costs O(m · α(n)), where α is the inverse Ackermann
function: at most 4 for any n you will ever see. Without either, a chain of unions can make
every `find` O(n), which the Route Finder kata checks.

Uses: connected components of a graph that only grows, "number of islands" when land is added
cell by cell, detecting a cycle when adding undirected edges, grouping duplicate accounts,
percolation, image segmentation, and Kruskal's algorithm.

## Minimum spanning trees

A **spanning tree** connects all vertices of an undirected graph with V − 1 edges and no cycles.
A **minimum** spanning tree (MST) has the smallest total weight: the cheapest way to connect
every site with cable, pipes or roads.

Both classic algorithms rely on the **cut property**: for any split of the vertices into two
groups, the lightest edge crossing the split belongs to some MST.

- **Kruskal**: sort the edges by weight and add each one unless its endpoints are already
  connected (union-find). O(E log E).
- **Prim**: grow one tree from a start vertex, always adding the lightest edge leaving it
  (a priority queue of edges). O(E log V); good for dense graphs.

Both are **greedy** algorithms (phase 4): a locally best choice that is provably globally best
here. An MST is not a shortest-path tree: it minimises the total, not the distance from one
vertex.

## In real systems

- **Routing protocols**: OSPF and IS-IS run Dijkstra on the link-state map of a network; BGP
  is closer to Bellman-Ford (distance vector with paths).
- **Maps and navigation** use A* plus heavy precomputation (contraction hierarchies) to answer
  continental queries in milliseconds.
- **Social and fraud systems** use union-find to merge identities that share an email, phone or
  card.
- **Network design** and **clustering** (single-linkage clustering is Kruskal stopped early)
  use MSTs.

## Summary

- Pick the shortest-path algorithm by the weights: BFS, 0-1 BFS, Dijkstra, Bellman-Ford, A*.
- Dijkstra needs non-negative weights; with Java's PriorityQueue use lazy deletion.
- Union-find with union by size and path compression is almost O(1) per operation.
- Kruskal = sort edges + union-find; Prim = grow a tree with a heap; both follow the cut
  property.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Shortest Paths": <https://algs4.cs.princeton.edu/44sp/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Union-Find": <https://algs4.cs.princeton.edu/15uf/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Minimum Spanning Trees": <https://algs4.cs.princeton.edu/43mst/>
- cp-algorithms, "Dijkstra Algorithm": <https://cp-algorithms.com/graph/dijkstra.html>
- cp-algorithms, "Disjoint Set Union": <https://cp-algorithms.com/data_structures/disjoint_set_union.html>
- cp-algorithms, "0-1 BFS": <https://cp-algorithms.com/graph/01_bfs.html>
- roadmap.sh, DSA topic "Dijkstra's Algorithm": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/dijkstras-algorithm@87UugvReiywF0E7Acos9U.md>
- roadmap.sh, DSA topic "Disjoint Set (Union-Find)": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/disjoint-set-union-find@La8XAT0BWvqRCrvQxtZIp.md>
