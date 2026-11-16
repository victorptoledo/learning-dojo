---
from: go
status: draft
concept: "Ranges and iterators"
from_way: "Range-over-func iterators (iter.Seq) with yield, and the slices and maps helper packages."
native_way: "Views and std::generator give lazy sequences; algorithms and adaptors are standard and compose with |."
sensei_tip: "A Go `iter.Seq[T]` that calls yield maps to a `std::generator<T>` that calls co_yield."
---

| Go | C++ |
| --- | --- |
| `func(yield func(T) bool)` | `std::generator<T>` with `co_yield` |
| `for x := range seq` | `for (auto x : gen)` |
| `slices.Collect(seq)` | `std::ranges::to<std::vector>()` |
| `slices.SortFunc(s, cmp)` | `std::ranges::sort(v, less, proj)` |
| hand-written Filter/Map helpers | `views::filter`, `views::transform` |
| `maps.Keys(m)` | `std::views::keys(m)` |
