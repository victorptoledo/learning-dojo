---
from: javascript
status: draft
concept: "Greedy algorithms"
from_way: "Sorting intervals with `sort()` and no comparator, which compares them as strings."
native_way: "`intervals.sort((a, b) => a[1] - b[1])` and one loop; for the active set, a small heap or, for the meeting-rooms count, a sweep over sorted start and end arrays, which needs no heap at all."
sensei_tip: "The sweep version of meeting rooms (sort starts, sort ends, walk both with two pointers) is the JavaScript-friendly answer because the language has no built-in priority queue."
---

| JavaScript | Java |
| --- | --- |
| `iv.sort((a, b) => a[1] - b[1])` | `Arrays.sort(iv, comparingInt(t -> t[1]))` |
| two sorted arrays + two pointers | sweep line |
| hand-written heap | `PriorityQueue` |
| `Math.max(...)` with spread | `Math.max` in a loop (spread fails on huge arrays) |
