---
from: javascript
status: draft
concept: "Ranges and generators"
from_way: "Array methods (map, filter) are eager; generator functions with yield and iterator helpers are lazy."
native_way: "Views are lazy by default, and std::generator with co_yield is the equivalent of function* with yield."
sensei_tip: "`arr.filter(p).map(f)` builds two arrays; the C++ pipeline builds none until you collect it."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `function* lines() { yield x }` | `std::generator<T> lines() { co_yield x; }` |
| `arr.filter(p).map(f)` (eager) | `v \| views::filter(p) \| views::transform(f)` (lazy) |
| `Array.from(iter)` | `std::ranges::to<std::vector>()` |
| iterator helpers `.take(n)` | `std::views::take(n)` |
| `arr.entries()` | `std::views::enumerate(v)` |
| `arr.sort((a, b) => a.age - b.age)` | `std::ranges::sort(v, {}, &T::age)` |
