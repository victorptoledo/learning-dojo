---
from: javascript
status: draft
concept: "Trees and heaps"
from_way: "Sorting an array after every insert to use it as a priority queue."
native_way: "JavaScript has no built-in heap or balanced tree. Write a small binary heap over an array (about 40 lines) or use a library; for ordered maps, a sorted array with binary search works for read-heavy data."
sensei_tip: "A heap class with `push`, `pop` and a comparator is a common interview warm-up in JavaScript precisely because the language lacks one; the array layout (children at 2i + 1 and 2i + 2) is identical to Java's PriorityQueue."
---

| JavaScript | Java |
| --- | --- |
| hand-written heap over an array | `PriorityQueue` |
| `arr.sort()` after each insert | O(n log n) per insert: avoid |
| sorted array + binary search | `TreeMap` for static data |
| DOM tree (`node.children`) | your own tree nodes |
| `{ val, left, right }` objects | `TreeNode` class |
