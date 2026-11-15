---
from: javascript
status: draft
concept: "Weighted graphs and connectivity"
from_way: "Sorting the frontier array on every step of Dijkstra because there is no built-in heap."
native_way: "Write a small binary heap (or use a library) and run the same lazy-deletion Dijkstra; union-find fits in two Int32Arrays. Map and routing libraries in the browser (and engines like OSRM behind them) run Dijkstra and A* variants."
sensei_tip: "`Infinity` is a real number in JavaScript, so `dist.fill(Infinity)` and `candidate < dist[v]` work without special cases. Typed arrays (`Int32Array`, `Float64Array`) keep big graphs compact."
---

| JavaScript | Java |
| --- | --- |
| hand-written heap | `PriorityQueue` |
| `new Float64Array(n).fill(Infinity)` | `long[]` filled with `Long.MAX_VALUE` |
| `new Int32Array(n)` parent | `int[] parent` |
| `edges.sort((a, b) => a[2] - b[2])` | `Arrays.sort(edges, comparingInt(e -> e[2]))` |
| array deque for 0-1 BFS | `ArrayDeque` |
