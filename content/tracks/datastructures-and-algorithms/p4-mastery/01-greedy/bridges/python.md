---
from: python
status: draft
concept: "Greedy algorithms"
from_way: "Sorting with key=len or by start because it reads naturally."
native_way: "`intervals.sort(key=lambda t: t[1])` plus one loop, and heapq for the active set, exactly as in Java. Tuples sort lexicographically, which gives tie-breaking for free."
sensei_tip: "`heapq.heapreplace(h, end)` pops the earliest end and pushes the new one in a single O(log n) step: the meeting-rooms loop in one call."
---

| Python | Java |
| --- | --- |
| `iv.sort(key=lambda t: t[1])` | `Arrays.sort(iv, comparingInt(t -> t[1]))` |
| `heapq.heappush(h, end)` | `PriorityQueue.add` |
| `heapq.heapreplace(h, end)` | `poll` + `add` |
| `sorted(iv)` (by start, then end) | `comparingInt(t -> t[0]).thenComparingInt(t -> t[1])` |
