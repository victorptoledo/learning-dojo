---
from: java
status: draft
concept: "Weighted graphs and connectivity"
from_way: "Calling PriorityQueue.remove(node) to update a distance, or recomputing connected components with a BFS after every new edge."
native_way: "Dijkstra uses a PriorityQueue<long[]> (or a record) with lazy deletion; union-find is two int arrays; Kruskal sorts an int[][] of edges with Arrays.sort and a Comparator.comparingInt. No JDK class does these for you, which is why they are interview staples."
sensei_tip: "Encode a heap entry as `long[] {distance, node}` or a `record Entry(long dist, int node)` and order it with `Comparator.comparingLong(Entry::dist)`. Use `long` distances: sums of int weights overflow quickly on big graphs."
---

| Need | Java |
| --- | --- |
| adjacency with weights | `List<List<Edge>>` with `record Edge(int to, int weight)` |
| Dijkstra queue | `PriorityQueue<long[]>(Comparator.comparingLong(e -> e[0]))` |
| infinite distance | `Long.MAX_VALUE` (check before adding) |
| union-find | `int[] parent, size` |
| Kruskal | `Arrays.sort(edges, Comparator.comparingInt(e -> e[2]))` |
| 0-1 BFS | `ArrayDeque` with `addFirst` / `addLast` |
