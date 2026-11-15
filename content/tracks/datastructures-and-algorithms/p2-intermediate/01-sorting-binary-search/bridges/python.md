---
from: python
status: draft
concept: "Sorting and searching"
from_way: "Writing cmp functions, or sorting the whole list to find a minimum or the top 10."
native_way: "sorted() and list.sort() are TimSort (stable) with a key function; heapq.nlargest finds the top k; the bisect module gives lower and upper bounds on sorted lists."
sensei_tip: "`bisect.bisect_left(a, x)` is exactly the lowerBound of this lesson and `bisect_right` the upperBound; since Python 3.10 both take a `key=` argument. Sorting by a tuple key, `key=lambda o: (o.customer, o.date)`, replaces chained comparators."
---

| Python | Java |
| --- | --- |
| `sorted(xs, key=f)` / `xs.sort(key=f)` | `list.sort(Comparator.comparing(f))` |
| `key=lambda o: (o.a, o.b)` | `comparing(A).thenComparing(B)` |
| `reverse=True` | `.reversed()` |
| `bisect.bisect_left(a, x)` | lowerBound (hand-written) |
| `bisect.insort(a, x)` | insert keeping order (O(n)) |
| `heapq.nlargest(k, xs)` | `PriorityQueue` of size k |
