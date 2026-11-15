---
from: python
status: draft
concept: "Weighted graphs and connectivity"
from_way: "Using a list and min() to pick the next vertex, which is O(V²) Dijkstra."
native_way: "heapq with (distance, node) tuples and lazy deletion is the standard Dijkstra, the same algorithm as the Java version. Union-find is two lists. For real work, networkx (third party) implements Dijkstra, A*, Bellman-Ford, MSTs and components."
sensei_tip: "`float('inf')` or `math.inf` is the natural infinite distance in Python, with no overflow worries. Tuples compare element by element, so `(dist, node)` orders by distance and breaks ties by node."
---

| Python | Java |
| --- | --- |
| `heapq.heappush(q, (d, v))` | `queue.add(new long[] {d, v})` |
| `if d > dist[v]: continue` | `if (top[0] > dist[v]) continue;` |
| `math.inf` | `Long.MAX_VALUE` |
| `parent = list(range(n))` | `int[] parent` |
| `edges.sort(key=lambda e: e[2])` | `Arrays.sort(edges, comparingInt(e -> e[2]))` |
| `networkx.dijkstra_path` (third party) | (no JDK equivalent) |
