---
from: go
status: draft
concept: "Moving instead of copying"
from_way: "Assigning a slice or map copies only its header, sharing the data; copying data needs copy or maps.Clone."
native_way: "Assigning a vector copies all its elements; moving one copies only its header and leaves the source empty, which is what Go does when you stop using the old variable."
sensei_tip: "A Go slice assignment is like a C++ move without emptying the source: in C++ either move (and forget the source) or copy (and pay)."
---

| Go | C++ |
| --- | --- |
| `b := a` (header copy, shared data) | `auto b = std::move(a);` (header move, a is emptied) |
| `b := slices.Clone(a)` | `auto b = a;` |
| escape analysis decides | you decide: copy, move or reference |
| returning a slice is cheap | returning a vector is cheap (elision or move) |
| no ownership | move-only types: `std::unique_ptr` |
