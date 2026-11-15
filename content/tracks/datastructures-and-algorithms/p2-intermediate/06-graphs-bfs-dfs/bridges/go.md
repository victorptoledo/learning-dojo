---
from: go
status: draft
concept: "Graph traversal"
from_way: "Thinking of Go's import rules only as a compiler annoyance."
native_way: "Go forbids import cycles, so the compiler topologically sorts your packages and rejects cycles: the graph algorithms of this lesson, enforced by the toolchain. In code, use map[string][]string or [][]int for adjacency, a slice as the BFS queue and a map[T]bool or []bool for visited."
sensei_tip: "`go list -deps ./...` prints the package graph in dependency order, and `go mod graph` prints the module graph as an edge list you can feed to any of the algorithms here."
---

| Go | Java |
| --- | --- |
| `map[string][]string` | `Map<String, List<String>>` |
| `[][]int` | `List<List<Integer>>` |
| `queue = append(queue, x)`; `x, queue = queue[0], queue[1:]` | `ArrayDeque.offer` / `poll` |
| `seen := make([]bool, n)` | `boolean[] seen` |
| import cycle error | circular dependency detection |
