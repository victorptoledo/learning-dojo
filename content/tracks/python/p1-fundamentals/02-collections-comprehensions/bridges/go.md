---
from: go
status: draft
concept: "Slices, maps and sets"
from_way: "Slices with append, map[K]V with the comma-ok idiom, map[K]struct{} as a set, sort.Slice or slices.SortFunc, and loops for every transformation."
native_way: "list, dict and a real set type, comprehensions for transformations, Counter and defaultdict, and sorted with a key function."
sensei_tip: "Python dicts iterate in insertion order, unlike Go maps, which are deliberately random. You can rely on that order in Python."
---

| Go | Python |
| --- | --- |
| `xs = append(xs, x)` | `xs.append(x)` |
| `v, ok := m[k]` | `v = m.get(k)` / `if k in m:` |
| `map[string]struct{}` | `set[str]` |
| `slices.SortFunc(xs, cmp)` | `xs.sort(key=...)` |
| a loop that builds a new slice | `[f(x) for x in xs if p(x)]` |
| `m[k]++` on a `map[string]int` | `Counter` or `m[k] = m.get(k, 0) + 1` |
| `xs[1:3]` (shares the array) | `xs[1:3]` (a copy) |
