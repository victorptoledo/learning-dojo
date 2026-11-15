---
from: java
status: draft
concept: "Trees and heaps"
from_way: "Using TreeMap and PriorityQueue as black boxes, or sorting a list after every insert to keep it ordered."
native_way: "TreeMap and TreeSet are red-black trees with O(log n) operations and rich navigation (floorKey, ceilingEntry, headMap, subMap, descendingMap); PriorityQueue is an array-based binary heap with O(log n) add and poll and O(n) remove(Object)."
sensei_tip: "`PriorityQueue`'s iterator does NOT return elements in order; only repeated `poll()` does. For a max-heap, pass `Comparator.reverseOrder()`. And `new PriorityQueue<>(collection)` heapifies in O(n), cheaper than n adds."
---

| Need | Java | Cost |
| --- | --- | --- |
| min-heap | `new PriorityQueue<>()` | add/poll O(log n), peek O(1) |
| max-heap | `new PriorityQueue<>(Comparator.reverseOrder())` | same |
| heapify | `new PriorityQueue<>(list)` | O(n) |
| sorted map | `TreeMap` | O(log n) |
| floor / ceiling | `floorKey`, `ceilingKey`, `lowerEntry`, `higherEntry` | O(log n) |
| range view | `subMap(from, true, to, false)` | O(log n + k) |
| concurrent sorted map | `ConcurrentSkipListMap` | O(log n) expected |
