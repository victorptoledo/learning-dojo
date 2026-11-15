---
from: javascript
status: draft
concept: "Sorting and searching"
from_way: "Calling arr.sort() on numbers and getting [1, 10, 2], because the default compares strings."
native_way: "Always pass a comparator for numbers: `arr.sort((a, b) => a - b)` (safe for doubles, no overflow). Array.prototype.sort is stable since ES2019 (V8 uses TimSort), and toSorted() returns a sorted copy. There is no built-in binary search: write lowerBound."
sensei_tip: "`arr.toSorted(cmp)` (ES2023) leaves the original alone, like Java's `stream().sorted()`. For typed arrays, `Int32Array.prototype.sort()` sorts numerically by default and is much faster."
---

| JavaScript | Java |
| --- | --- |
| `arr.sort((a, b) => a - b)` | `Arrays.sort` / `List.sort` |
| `arr.toSorted(cmp)` | `stream().sorted(cmp).toList()` |
| `(a, b) => a.x - b.x \|\| a.y - b.y` | `comparing(X).thenComparing(Y)` |
| `int32Array.sort()` | `Arrays.sort(int[])` |
| hand-written lowerBound | hand-written lowerBound / `Arrays.binarySearch` |
| `a.localeCompare(b)` | `Collator.compare` |
