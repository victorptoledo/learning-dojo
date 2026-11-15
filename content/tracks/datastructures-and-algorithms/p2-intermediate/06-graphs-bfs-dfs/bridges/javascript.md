---
from: javascript
status: draft
concept: "Graph traversal"
from_way: "Walking object references recursively (like a JSON tree) and forgetting that graphs can have cycles."
native_way: "A Map from node to an array of neighbours is the adjacency list, an array with a head index is the BFS queue, and a Set holds visited nodes. Module bundlers (webpack, Rollup) and npm build exactly these graphs to order and deduplicate modules."
sensei_tip: "`JSON.stringify` throws on circular references for the same reason a naive DFS loops forever: there is no visited set. `structuredClone` handles cycles because it keeps one."
---

| JavaScript | Java |
| --- | --- |
| `Map<node, node[]>` | `Map<K, List<K>>` |
| `queue[head++]` | `ArrayDeque.poll` |
| `new Set()` visited | `HashSet` |
| `stack.push` / `stack.pop` | `ArrayDeque.push` / `pop` |
| bundler module graph | Maven dependency graph |
