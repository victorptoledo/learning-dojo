---
from: go
status: draft
concept: "Trees and heaps"
from_way: "Looking for a TreeMap in the standard library."
native_way: "container/heap provides the heap algorithms over any type that implements heap.Interface (Len, Less, Swap, Push, Pop); there is no balanced tree or sorted map in the standard library, so you sort keys on demand or use a third-party B-tree (such as google/btree)."
sensei_tip: "With container/heap you write the slice type and five methods once; heap.Init is the O(n) heapify and heap.Fix(h, i) updates an element's priority in O(log n) if you track its index, which Java's PriorityQueue cannot do."
---

| Go | Java |
| --- | --- |
| `container/heap` + `heap.Interface` | `PriorityQueue` |
| `heap.Init(h)` | `new PriorityQueue<>(list)` |
| `heap.Push(h, x)` / `heap.Pop(h)` | `add` / `poll` |
| `heap.Fix(h, i)` | (remove + add) |
| `slices.Sorted(maps.Keys(m))` | iterate a `TreeMap` |
| `github.com/google/btree` | `TreeMap` |
