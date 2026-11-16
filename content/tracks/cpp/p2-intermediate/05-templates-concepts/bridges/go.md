---
from: go
status: draft
concept: "Generics and constraints"
from_way: "Type parameters with interface constraints such as cmp.Ordered and ~int | ~float64; one implementation shared through dictionaries."
native_way: "Templates with concepts as constraints; each instantiation is separate code, fully optimised for its type."
sensei_tip: "`[T cmp.Ordered]` becomes `template <std::totally_ordered T>`, and a Go type set `~int | ~float64` becomes a concept with ||."
---

| Go | C++ |
| --- | --- |
| `func Max[T cmp.Ordered](a, b T) T` | `template <std::totally_ordered T> T max(T a, T b)` |
| `interface{ ~int \| ~float64 }` | `concept Number = std::integral<T> \|\| std::floating_point<T>;` |
| methods in constraints | requires-expressions |
| no generic methods | member function templates |
| type switch | `if constexpr` |
| one shape-based implementation | one instantiation per type |
