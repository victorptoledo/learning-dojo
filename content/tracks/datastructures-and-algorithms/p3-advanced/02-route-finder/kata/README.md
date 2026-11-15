# Route Finder

Shortest paths and connectivity, the way routing services, network tools and map software use
them. You write `UnionFind` and the four static methods of `Routes`. `java.util.PriorityQueue`,
`ArrayDeque` and the other JDK collections are allowed.

## `UnionFind(int n)`

Elements `0` to `n - 1`, each in its own set at first.

| Method | Does |
| --- | --- |
| `find(x)` | the representative of x's set, with **path compression** |
| `union(a, b)` | merges the two sets, the smaller under the larger (**union by size**); `false` if they were already one set |
| `connected(a, b)` | whether a and b are in the same set |
| `size(x)` | the number of elements in x's set |
| `components()` | the number of sets |

An element outside `0..n-1` throws `IndexOutOfBoundsException` (an array access does that for
you). The hidden tests run a million operations shaped so that a union-find without either
optimisation builds a chain a million long.

## `Routes`

Edges are `int[] {from, to, weight}` with nodes `0..n-1`. A **negative weight** throws
`IllegalArgumentException`.

| Method | Returns |
| --- | --- |
| `gridDistance(grid)` | the fewest steps (up, down, left, right) from `S` to `E` in a grid of `.` (open), `#` (wall), `S` and `E`; `-1` if `E` cannot be reached. A grid without `S` or `E` throws `IllegalArgumentException`. |
| `shortestDistances(n, edges, source)` | the length of the shortest **directed** path from `source` to every node, `-1` where there is none |
| `shortestPath(n, edges, source, target)` | the nodes of one shortest directed path, `source` first; `[source]` when they are equal, empty when there is no path |
| `minimumSpanningTree(n, edges)` | the total weight of a minimum spanning tree with the edges taken as **undirected**; `-1` if the graph is not connected; `0` for `n <= 1` |

```text
grid  S.#.      gridDistance = 6
      .##E      (down, down, right, right, right, up)
      ....
```

- `gridDistance` is a **BFS**: every step costs 1.
- `shortestDistances` and `shortestPath` are **Dijkstra** with a priority queue of
  `(distance, node)`; skip entries whose distance is older than the best known one ("lazy
  deletion"). For the path, remember the previous node of every improved distance.
- `minimumSpanningTree` is **Kruskal**: sort the edges by weight and keep each edge whose ends
  are not yet connected, using your `UnionFind`.

The hidden tests compare with slow references on small random graphs, then use 200,000 nodes and
a million edges, and a 2,000 × 2,000 grid.
