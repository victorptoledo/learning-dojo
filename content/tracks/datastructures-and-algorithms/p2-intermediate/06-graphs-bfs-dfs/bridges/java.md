---
from: java
status: draft
concept: "Graph traversal"
from_way: "Forcing every relationship into flat relational tables and walking them with nested queries or recursive method calls on entities."
native_way: "Graphs represent complex connections: social networks, delivery routes, package dependencies. Store them as adjacency lists (List<List<Integer>> or Map<String, List<String>>), traverse with an ArrayDeque used as a queue (BFS) or a stack (DFS), and keep visited state in a boolean[] or a HashSet."
sensei_tip: "BFS finds the shortest path in unweighted graphs with a queue; DFS goes deep with a stack or recursion. For grids, `int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}}` and an `r * cols + c` index turn the grid into a graph with no extra structure."
---

| Need | Java |
| --- | --- |
| numbered vertices | `List<List<Integer>> graph` |
| named vertices | `Map<String, List<String>> graph` with `computeIfAbsent` |
| BFS queue | `ArrayDeque<Integer>` with `offer` / `poll` |
| DFS stack | `ArrayDeque<Integer>` with `push` / `pop` |
| visited | `boolean[]`, `int[] dist` filled with -1, or `HashSet` |
| topological order, smallest first | Kahn with a `PriorityQueue<String>` |
| framework example | Spring detects circular bean references in its dependency graph |
