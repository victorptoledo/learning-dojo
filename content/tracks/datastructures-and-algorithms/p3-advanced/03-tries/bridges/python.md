---
from: python
status: draft
concept: "Tries and prefix search"
from_way: "`[w for w in words if w.startswith(p)]` on every request."
native_way: "A trie in Python is often a nested dict: `node = node.setdefault(ch, {})`, with a sentinel key such as '$' to mark the end of a word. For sorted data, bisect on a sorted list finds the prefix range in O(log n)."
sensei_tip: "`bisect_left(words, p)` and `bisect_left(words, p + '\\uffff')` bracket every word with prefix p in a sorted list: a two-line prefix search when you do not need counts or updates."
---

| Python | Java |
| --- | --- |
| nested `dict` trie | `Map<Character, Node>` nodes |
| `node.setdefault(ch, {})` | `computeIfAbsent(ch, k -> new Node())` |
| `'$' in node` end marker | `boolean end` / `long freq` |
| `bisect` on a sorted list | `TreeMap.subMap` |
| `heapq.nlargest(k, ...)` | `PriorityQueue` of size k |
