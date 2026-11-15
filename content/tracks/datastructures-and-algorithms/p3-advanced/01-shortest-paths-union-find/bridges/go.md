---
from: go
status: draft
concept: "Weighted graphs and connectivity"
from_way: "Avoiding container/heap because of its interface boilerplate."
native_way: "A slice type with Len, Less, Swap, Push and Pop plugs into container/heap for Dijkstra; with heap.Fix you can even implement a real decrease-key by tracking each node's index. Union-find is two []int slices."
sensei_tip: "`math.MaxInt64` is the infinite distance; with an indexed heap (store the heap index of each node), `heap.Fix(h, idx[v])` updates a distance in O(log n) without duplicate entries."
---

| Go | Java |
| --- | --- |
| `container/heap` with an item slice | `PriorityQueue` |
| `heap.Fix(h, i)` | (lazy deletion instead) |
| `math.MaxInt64` | `Long.MAX_VALUE` |
| `parent := make([]int, n)` | `int[] parent` |
| `slices.SortFunc(edges, ...)` | `Arrays.sort(edges, ...)` |
