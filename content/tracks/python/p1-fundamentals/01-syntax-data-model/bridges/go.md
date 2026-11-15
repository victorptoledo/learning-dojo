---
from: go
status: draft
concept: "Names, objects and loose control flow"
from_way: "Values copied on assignment unless you use pointers, slices and maps sharing their backing data, explicit types, one for loop and switch without fallthrough."
native_way: "Every name is a reference to an object, so assignment shares; immutable types make that safe; truthiness, for over any iterable, while and match."
sensei_tip: "A Python list behaves like a Go slice you never copy by accident: b = a shares it completely. Copy with list(a) when you mean a new one."
---

| Go | Python |
| --- | --- |
| `b := a` (copy of a struct) | `b = a` (same object) |
| `b := slices.Clone(a)` | `b = list(a)` / `a.copy()` |
| `for i, x := range xs` | `for i, x in enumerate(xs):` |
| `for cond { }` | `while cond:` |
| `fmt.Sprintf("%s owes %.2f", n, x)` | `f"{n} owes {x:.2f}"` |
| `switch v := x.(type)` | `match x: case int(): ... case str(): ...` |
| `nil` | `None` |
| `math/big` | `int` (always unbounded) |
