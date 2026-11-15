---
from: java
status: draft
concept: "Collections and stream pipelines"
from_way: "List, Set and Map interfaces with ArrayList, HashSet and HashMap, Collectors.groupingBy and counting, and streams with filter and map."
native_way: "Built-in list, set and dict with literals, Counter and defaultdict for counting and grouping, and comprehensions instead of most stream pipelines."
sensei_tip: "A comprehension is a stream that ends in toList(), toSet() or toMap() in one line. Use Counter where you would write groupingBy(identity(), counting())."
---

| Java | Python |
| --- | --- |
| `new ArrayList<>(List.of(1, 2))` | `[1, 2]` |
| `map.getOrDefault(k, 0)` | `d.get(k, 0)` |
| `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)` | `groups = defaultdict(list); groups[k].append(v)` |
| `groupingBy(w -> w, counting())` | `Counter(words)` |
| `xs.stream().filter(p).map(f).toList()` | `[f(x) for x in xs if p(x)]` |
| `Comparator.comparing(P::age).thenComparing(P::name)` | `key=lambda p: (p.age, p.name)` |
| `ArrayDeque` | `collections.deque` |
| `record Pair(int a, int b)` as a key | the tuple `(a, b)` |
