---
from: python
status: draft
concept: "Graph traversal"
from_way: "Writing recursive DFS on large graphs and hitting the 1,000-frame recursion limit."
native_way: "A dict of lists (often a defaultdict(list)) is the adjacency list, collections.deque is the BFS queue, and a set holds visited nodes. The standard library even has graphlib.TopologicalSorter, which implements Kahn's algorithm and raises CycleError on cycles."
sensei_tip: "`graphlib.TopologicalSorter(graph).static_order()` gives a topological order in one line, and its `prepare()` / `get_ready()` / `done()` API schedules tasks in parallel as they become ready, like a build tool."
---

| Python | Java |
| --- | --- |
| `defaultdict(list)` | `Map<K, List<K>>` with `computeIfAbsent` |
| `deque` + `popleft()` | `ArrayDeque.poll` |
| `seen = set()` | `HashSet` / `boolean[]` |
| `graphlib.TopologicalSorter` | Kahn's algorithm by hand |
| `graphlib.CycleError` | `IllegalStateException` of your own |
| recursion limit ~1,000 | larger, but still limited |
