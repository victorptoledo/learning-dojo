---
from: python
status: draft
concept: "Two pointers and sliding windows"
from_way: "Slicing a fresh list for every window, `sum(a[i:i+k])`, which is O(k) per window and allocates."
native_way: "Keep indexes and a running aggregate, exactly as in Java. Python ints never overflow, and collections.Counter and defaultdict make window counts short."
sensei_tip: "`itertools.accumulate(a, initial=0)` gives the prefix sums with the leading 0, and a `Counter` keyed by prefix value counts subarrays with sum k in one loop. Avoid `a[i:j]` inside the loop: each slice copies."
---

| Pattern | Python | Java |
| --- | --- | --- |
| opposite ends | `i, j = 0, len(a) - 1` | `int i = 0, j = n - 1` |
| window counts | `Counter()` / `defaultdict(int)` | `HashMap.merge` |
| window max | `collections.deque` of indexes | `ArrayDeque` |
| prefix sums | `list(accumulate(a, initial=0))` | `long[] prefix` |
| count by prefix | `seen[prefix - k]` on a `Counter` | `getOrDefault(prefix - k, 0)` |
