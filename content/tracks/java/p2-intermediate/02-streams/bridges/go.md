---
from: go
status: draft
concept: "Pipelines over collections"
from_way: "for range loops and helper functions, with iterators in Go 1.23."
native_way: "Declarative stream pipelines with collectors."
sensei_tip: "Go prefers loops; Java offers both. A stream with groupingBy replaces a loop that fills a map, but a plain loop is still fine when you need early exits or checked exceptions."
---

| Go | Java 25 |
| --- | --- |
| for _, x := range xs { if p(x) { ... } } | xs.stream().filter(p) |
| m[k] += v in a loop | groupingBy(k, summingLong(v)) |
| slices.ContainsFunc | anyMatch |
| slices.SortFunc | sorted(Comparator...) |
