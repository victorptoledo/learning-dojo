---
from: javascript
status: draft
concept: "Coding interviews"
from_way: "Interviewing in JavaScript and losing time writing a heap or a queue from scratch."
native_way: "Know the gaps up front: no heap, no deque, no sorted map, default sort compares strings. Say how you handle them (a quick heap, an array with a head index, a numeric comparator) and the interviewer will usually accept a simplified helper."
sensei_tip: "Ask early whether you may assume a `MinHeap` class exists. Many interviewers allow it, which saves ten minutes; if not, write a compact 30-line version you have practised."
---

| Need | JavaScript | Java |
| --- | --- | --- |
| count | `map.set(k, (map.get(k) ?? 0) + 1)` | `merge` |
| queue | array + head index | `ArrayDeque` |
| heap | your own class (or assume one) | `PriorityQueue` |
| numeric sort | `a.sort((x, y) => x - y)` | `Arrays.sort` |
| set | `new Set()` | `HashSet` |
