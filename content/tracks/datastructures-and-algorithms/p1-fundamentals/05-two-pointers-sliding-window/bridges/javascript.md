---
from: javascript
status: draft
concept: "Two pointers and sliding windows"
from_way: "Chaining slice, reduce and filter for every window, which is O(n·k) and allocates arrays."
native_way: "Write the loop with two indexes and a running aggregate; use a Map for counts. Numbers are doubles, so sums are exact only up to 2^53; use BigInt beyond that."
sensei_tip: "`map.set(k, (map.get(k) ?? 0) + 1)` is the counting idiom, and for a character window over ASCII text an `Int32Array(128)` is faster than a Map."
---

| Pattern | JavaScript | Java |
| --- | --- | --- |
| opposite ends | `let i = 0, j = a.length - 1` | `int i = 0, j = n - 1` |
| window counts | `Map` or `Int32Array(128)` | `HashMap` or `int[128]` |
| window max | array + head index as a deque | `ArrayDeque` |
| prefix sums | `Float64Array(n + 1)` or `BigInt` | `long[] prefix` |
| count by prefix | `seen.get(prefix - k) ?? 0` | `getOrDefault` |
