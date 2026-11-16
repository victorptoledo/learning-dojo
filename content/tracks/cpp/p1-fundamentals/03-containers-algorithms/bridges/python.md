---
from: python
status: draft
concept: "Collections and iteration"
from_way: "list, dict, set and comprehensions; sorted(key=...); dict keeps insertion order."
native_way: "std::vector, std::unordered_map and std::map, std::set; algorithms with comparators and projections; unordered_map has no order, map is sorted."
sensei_tip: "A Python dict remembers insertion order; std::unordered_map does not and std::map sorts by key. Keep a vector alongside if order matters."
---

| Python | C++ |
| --- | --- |
| `list` | `std::vector` |
| `dict` | `std::unordered_map` (or `std::map` for sorted keys) |
| `set` | `std::unordered_set` / `std::set` |
| `d.get(k, 0)` | `find` or `contains`; `d[k]` inserts a default |
| `sorted(xs, key=f)` | `std::ranges::sort(xs, {}, f)` |
| `[x for x in xs if p(x)]` | `std::views::filter` or `std::ranges::copy_if` |
| `xs[i]` raises IndexError | `v[i]` unchecked; `v.at(i)` throws |
