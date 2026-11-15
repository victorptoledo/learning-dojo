---
from: python
status: draft
concept: "Built-in types and collections"
from_way: "Dynamic types, None for missing values, lists that own their items, dicts that keep insertion order, and str as a sequence of code points."
native_way: "Static types with zero values, slices that may share a backing array, maps with random iteration order, and string as bytes where range yields runes."
sensei_tip: "In Python, a[1:3] copies; in Go, s[1:3] does not. That single difference explains most slice bugs Python developers write in Go."
---

| Python | Go |
| --- | --- |
| `xs = []` | `var xs []int` |
| `xs.append(4)` | `xs = append(xs, 4)` |
| `xs[1:3]` (a copy) | `xs[1:3]` (a view) or `slices.Clone(xs[1:3])` |
| `d = {"a": 1}` | `d := map[string]int{"a": 1}` |
| `d.get("b", 0)` | `d["b"]` |
| `"b" in d` | `_, ok := d["b"]` |
| `for k in sorted(d)` | `for _, k := range slices.Sorted(maps.Keys(d))` |
| `len("héllo") == 5` | `len("héllo") == 6`, `utf8.RuneCountInString` is 5 |
| `int("3")` | `strconv.Atoi("3")` returns `(3, nil)` |
