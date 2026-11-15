---
from: go
status: draft
concept: "Slices, maps and loops since Go 1.21"
from_way: "Hand-written loops for Contains, Index, Max and Reverse, sort.Slice with an index closure, collecting map keys by hand, a copy of the loop variable before every closure, and for i := 0; i < n; i++ for counting."
native_way: "The generic slices and maps packages, min and max built-ins, clear, range over integers, per-iteration loop variables (1.22) and iterator helpers such as slices.Sorted(maps.Keys(m))."
sensei_tip: "go fix ./... applies most of these rewrites for you (rangeint, minmax, slicescontains, mapsloop). Review the diff and keep it."
---

| Older Go | Go 1.27 |
| --- | --- |
| a loop that returns true when found | `slices.Contains(xs, x)` |
| `sort.Slice(xs, func(i, j int) bool { ... })` | `slices.SortFunc(xs, func(a, b T) int { ... })` |
| `if a > b { m = a } else { m = b }` | `m := max(a, b)` |
| `for k := range m { delete(m, k) }` | `clear(m)` |
| `keys := make(...); for k := range m { keys = append(keys, k) }; sort.Strings(keys)` | `keys := slices.Sorted(maps.Keys(m))` |
| `for i := 0; i < n; i++` | `for i := range n` |
| `v := v` before a closure in a loop | not needed since Go 1.22 |
