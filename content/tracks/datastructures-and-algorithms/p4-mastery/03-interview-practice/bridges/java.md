---
from: java
status: draft
concept: "Coding interviews in Java"
from_way: "Writing interview code as production Java, with interfaces, factories and getters, or fighting the verbosity of the language under time pressure."
native_way: "Keep interview Java compact and idiomatic: var, records for small value types, ArrayDeque, HashMap.merge and getOrDefault, PriorityQueue with Comparator.comparingInt, Arrays.sort and List.of. Mention production concerns out loud instead of coding them."
sensei_tip: "Learn these by heart so you never pause on them: `map.merge(k, 1, Integer::sum)`, `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)`, `new PriorityQueue<>((a, b) -> Integer.compare(b[1], a[1]))`, `Arrays.fill(dist, -1)`, `deque.pollFirst()`, `list.removeLast()`."
---

| Interview need | Java idiom |
| --- | --- |
| count | `map.merge(k, 1, Integer::sum)` |
| group | `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)` |
| stack / queue | `ArrayDeque` |
| top k | `PriorityQueue` with `Comparator.comparingInt` |
| small value type | `record Point(int r, int c) {}` |
| sort pairs | `Arrays.sort(a, (x, y) -> x[0] != y[0] ? Integer.compare(x[0], y[0]) : Integer.compare(x[1], y[1]))` |
| overflow-safe | `long`, `Math.addExact` |
