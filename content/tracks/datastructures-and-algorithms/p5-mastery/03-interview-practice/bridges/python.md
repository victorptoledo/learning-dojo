---
from: python
status: draft
concept: "Coding interviews"
from_way: "Choosing Python for interviews and then relying on one-liners the interviewer cannot follow."
native_way: "Python is the most compact interview language: Counter, defaultdict, deque, heapq, bisect and itertools cover most patterns. The process (clarify, brute force, optimise, test) is identical to Java; only the code is shorter."
sensei_tip: "If the role is a Java role, interview in Java unless told otherwise: fluency with ArrayDeque and PriorityQueue under pressure is part of the signal. If you use Python, still say what each library call costs."
---

| Need | Python | Java |
| --- | --- | --- |
| count | `Counter(xs)` | `map.merge(k, 1, Integer::sum)` |
| group | `defaultdict(list)` | `computeIfAbsent` |
| queue | `deque` | `ArrayDeque` |
| top k | `heapq.nlargest(k, xs)` | `PriorityQueue` of size k |
| lower bound | `bisect_left` | hand-written |
| memo | `@cache` | `HashMap` / array |
