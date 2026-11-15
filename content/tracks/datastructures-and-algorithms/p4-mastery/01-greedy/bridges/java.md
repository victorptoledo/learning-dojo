---
from: java
status: draft
concept: "Greedy algorithms"
from_way: "Sorting by whatever field is at hand and hoping the result is optimal."
native_way: "A greedy algorithm in Java is usually Arrays.sort with the right Comparator plus one loop, sometimes with a PriorityQueue for the 'currently active' set. The thinking is in the choice of key and the argument for it."
sensei_tip: "`Comparator.comparingInt((int[] t) -> t[1])` sorts intervals by end; the explicit `(int[] t)` type is needed when the lambda feeds `comparingInt` directly into `Arrays.sort`. Use half-open intervals [start, end) so touching intervals do not overlap."
---

| Need | Java |
| --- | --- |
| sort intervals by end | `Arrays.sort(iv, Comparator.comparingInt(t -> t[1]))` |
| active intervals | `PriorityQueue<Integer>` of end times |
| merge intervals | `List<int[]>` built while scanning |
| Huffman tree | `PriorityQueue<Node>` by frequency |
| sweep line | sorted `int[] starts`, `int[] ends` and two pointers |
