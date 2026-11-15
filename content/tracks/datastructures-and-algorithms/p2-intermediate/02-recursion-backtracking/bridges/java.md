---
from: java
status: draft
concept: "Recursion and backtracking"
from_way: "Writing recursion only when a textbook asks for it, and being surprised by StackOverflowError on real data."
native_way: "Recurse where the depth is logarithmic or small, keep one mutable path (an ArrayList with add and removeLast) in backtracking, and switch to an ArrayDeque stack where the depth could be n. Java has no tail-call elimination."
sensei_tip: "In backtracking, copy the path only when you record a result: `out.add(List.copyOf(path))`. And `path.removeLast()` (Java 21+, SequencedCollection) reads exactly like the unchoose step."
---

| Pattern | Java |
| --- | --- |
| shared path | `List<Integer> path = new ArrayList<>();` |
| choose / unchoose | `path.add(x); recurse(...); path.removeLast();` |
| record a result | `out.add(List.copyOf(path))` |
| used flags (permutations) | `boolean[] used` |
| explicit stack instead of recursion | `Deque<Frame> stack = new ArrayDeque<>()` |
| bigger stack, last resort | `new Thread(null, task, "deep", 512L << 20).start()` |
| parallel divide and conquer | `RecursiveTask<T>` on a `ForkJoinPool` |
