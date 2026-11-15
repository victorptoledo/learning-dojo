---
from: go
status: draft
concept: "Greedy algorithms"
from_way: "Implementing sort.Interface for an interval type every time."
native_way: "`slices.SortFunc(iv, func(a, b [2]int) int { return cmp.Compare(a[1], b[1]) })` and one loop; container/heap for the active set. The Kubernetes scheduler, written in Go, scores nodes and places pods greedily."
sensei_tip: "Arrays like `[2]int` are comparable values in Go, so intervals stored as `[][2]int` sort, copy and compare without pointer surprises."
---

| Go | Java |
| --- | --- |
| `slices.SortFunc(iv, ...)` | `Arrays.sort(iv, comparator)` |
| `[][2]int` intervals | `int[][]` |
| `container/heap` | `PriorityQueue` |
| `cmp.Compare` | `Integer.compare` |
