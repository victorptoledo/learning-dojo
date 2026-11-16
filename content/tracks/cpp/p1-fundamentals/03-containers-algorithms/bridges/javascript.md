---
from: javascript
status: draft
concept: "Collections and iteration"
from_way: "Array, Map, Set and their methods (map, filter, sort); sort compares as strings by default."
native_way: "std::vector, std::unordered_map, std::map, std::set and algorithms; sort uses operator< or your strict comparator."
sensei_tip: "JavaScript's sort comparator returns a number; a C++ comparator returns a bool meaning 'a comes before b', and must be strict."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `[]` / `Array` | `std::vector<T>` |
| `Map` | `std::unordered_map` / `std::map` |
| `Set` | `std::unordered_set` / `std::set` |
| `arr.sort((a, b) => a - b)` | `std::ranges::sort(v)` |
| `arr.filter(p)` | `std::views::filter(p)` / `std::erase_if` on a copy |
| `arr[99]` gives undefined | `v[99]` is UB; `v.at(99)` throws |
| `map.get(k)` undefined | `find` returns `end()` |
