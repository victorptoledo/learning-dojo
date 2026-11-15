---
from: python
status: draft
concept: "Trees and heaps"
from_way: "Looking for a TreeMap in the standard library and keeping a list sorted with bisect.insort."
native_way: "heapq turns a plain list into a binary min-heap (heappush, heappop, heapify in O(n), nlargest). The standard library has no balanced tree; the third-party sortedcontainers package (SortedList, SortedDict) fills that role, built on lists of lists rather than nodes."
sensei_tip: "For a max-heap with heapq, push negated keys or tuples like (-score, name). Tuples compare element by element, so `(priority, counter, item)` gives stable ordering and avoids comparing items."
---

| Python | Java |
| --- | --- |
| `heapq.heappush(h, x)` / `heappop(h)` | `PriorityQueue.add` / `poll` |
| `heapq.heapify(lst)` | `new PriorityQueue<>(list)` |
| `heapq.nlargest(k, xs)` | heap of size k |
| `heapq.merge(*sorted_lists)` | k-way merge with a heap |
| `sortedcontainers.SortedDict` (third party) | `TreeMap` |
| `bisect.insort(lst, x)` | O(n) insert, not a tree |
