# Graphs: BFS, DFS and Topological Sort

A **graph** is a set of **vertices** (nodes) and **edges** between them. Whenever a problem talks
about connections (people who follow each other, services that call each other, packages that
depend on each other, cells of a grid next to each other, pages that link to each other) it is a
graph problem, and two traversals, **BFS** and **DFS**, answer most of the questions.

## Kinds of graphs

| Kind | Edges | Example |
| --- | --- | --- |
| undirected | both ways | friendships, road network with two-way streets |
| directed | one way | follows, calls, "depends on", links |
| weighted | a cost per edge | distances, latencies, prices (next phase) |
| DAG | directed, no cycles | build dependencies, course prerequisites, spreadsheets |
| tree | connected, no cycles, V − 1 edges | hierarchies |
| implicit | edges computed on the fly | grid cells, game states, word ladders |

## Representing a graph

| | Adjacency list | Adjacency matrix | Edge list |
| --- | --- | --- | --- |
| Memory | O(V + E) | O(V²) | O(E) |
| Neighbours of v | O(deg v) | O(V) | O(E) |
| Is (u, v) an edge? | O(deg u), O(1) with sets | O(1) | O(E) |
| Good for | sparse graphs: almost all real ones | dense or tiny graphs | Kruskal, input formats |

In Java an adjacency list is a `List<List<Integer>>` when vertices are numbered, or a
`Map<String, List<String>>` when they have names. Grids need no structure at all: the
neighbours of `(r, c)` are computed with a direction array.

## Breadth-first search

BFS explores in **rings**: the start, then everything one edge away, then two edges away, using a
**queue**:

```java
int[] dist = new int[n];
Arrays.fill(dist, -1);
Deque<Integer> queue = new ArrayDeque<>();
dist[start] = 0;
queue.add(start);
while (!queue.isEmpty()) {
    int v = queue.poll();
    for (int w : graph.get(v)) {
        if (dist[w] == -1) {          // mark when enqueued, not when dequeued
            dist[w] = dist[v] + 1;
            queue.add(w);
        }
    }
}
```

- Cost **O(V + E)**.
- Because rings are processed in order, `dist[w]` is the **fewest edges** from the start: BFS
  finds shortest paths in **unweighted** graphs. Keep a `parent[]` to rebuild the path.
- **Multi-source BFS**: put several starts in the queue at distance 0 ("distance to the nearest
  hospital", "rotting oranges").
- Level by level: read `queue.size()` at the start of each round.

## Depth-first search

DFS goes as deep as it can, then backtracks, using recursion or an explicit **stack**:

```java
void dfs(int v, boolean[] seen) {
    seen[v] = true;
    for (int w : graph.get(v)) if (!seen[w]) dfs(w, seen);
}
```

- Cost **O(V + E)**. Recursion depth can reach V: on large graphs use an explicit stack.
- **Reachability** and **connected components**: start a DFS (or BFS) from every unvisited
  vertex; each start is a new component. Flood fill and "number of islands" are this on a grid.
- **Cycle detection** in a directed graph uses three colours: white (unseen), gray (on the
  current path), black (finished). An edge to a **gray** vertex is a back edge: a cycle. In an
  undirected graph, any edge to a visited vertex other than your parent is a cycle.
- DFS also underlies bipartite checks (two-colouring), articulation points and bridges (Tarjan)
  and strongly connected components (Kosaraju, Tarjan).

### BFS or DFS?

| Question | Use |
| --- | --- |
| fewest steps, nearest something, levels | BFS |
| is it reachable, how many components, flood fill | either (DFS is shorter to write) |
| cycle in a directed graph, topological order by finishing time | DFS |
| explore all paths, puzzles | DFS / backtracking |
| very deep graphs | BFS, or DFS with an explicit stack |

## Topological sort

A **topological order** of a DAG lists every vertex before the vertices that depend on it:
compile before test, a prerequisite before the course. It exists **exactly when there is no
cycle**.

**Kahn's algorithm** (BFS flavour):

1. Count each vertex's **in-degree** (how many edges point to it).
2. Queue every vertex with in-degree 0.
3. Repeatedly take one out, append it to the order, and decrement the in-degree of its
   successors; queue those that reach 0.
4. If the order has fewer than V vertices, the rest are stuck on a **cycle**.

Use a `PriorityQueue` instead of a queue to get the lexicographically smallest order, which the
Intermediate checkpoint kata asks for. The **DFS** alternative appends each vertex when it
finishes (post-order) and reverses the list.

Once you have the order, many DAG problems become a single pass: **longest path** (the critical
path of a build or a project plan), number of paths, earliest start times.

## In real systems

- **Build tools** (Maven, Gradle, Bazel), **package managers** (npm, apt) and **workflow
  engines** (Airflow DAGs) topologically sort tasks and refuse cycles.
- **Spring** builds a dependency graph of beans and reports circular references.
- **Garbage collectors** mark reachable objects from the roots with a graph traversal.
- **Web crawlers** are BFS with a politeness queue; **social networks** compute "people you may
  know" from 2-hop neighbourhoods.
- **Databases** detect deadlocks by finding cycles in the wait-for graph.
- **Graph databases** (Neo4j) and SQL recursive CTEs (`WITH RECURSIVE`) run traversals over
  stored edges.

## Summary

- Model connections as a graph; use adjacency lists for sparse graphs.
- BFS with a queue gives unweighted shortest paths in O(V + E); mark nodes when you enqueue them.
- DFS finds components and, with three colours, cycles in directed graphs.
- Topological sort exists exactly for DAGs; Kahn's algorithm finds it and detects cycles.
- Deep graphs need iterative traversals.

## Further reading

- Sedgewick and Wayne, *Algorithms*, 4th ed., "Undirected Graphs": <https://algs4.cs.princeton.edu/41graph/>
- Sedgewick and Wayne, *Algorithms*, 4th ed., "Directed Graphs" (including topological sort): <https://algs4.cs.princeton.edu/42digraph/>
- cp-algorithms, "Breadth-first search": <https://cp-algorithms.com/graph/breadth-first-search.html>
- cp-algorithms, "Depth First Search": <https://cp-algorithms.com/graph/depth-first-search.html>
- cp-algorithms, "Topological Sorting": <https://cp-algorithms.com/graph/topological-sort.html>
- roadmap.sh, DSA topic "Graph Data Structures": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/graph-data-structures@j_r1tB16bA7R3yDAWN7Oj.md>
- roadmap.sh, DSA topic "Breadth First Search": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/datastructures-and-algorithms/content/breadth-first-search@c7LdXJPv-5M-NPKSCXYuB.md>
