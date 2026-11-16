---
from: python
status: draft
concept: "Moving instead of copying"
from_way: "Assignment binds another name to the same object; copy.deepcopy copies explicitly."
native_way: "Assignment copies; std::move turns a copy into a cheap transfer of the internal buffers, leaving the source empty."
sensei_tip: "In Python sharing is free and copying is explicit; in C++ copying is the default and moving is how you make it free."
---

| Python | C++ |
| --- | --- |
| `b = a` (shared) | `auto b = a;` (copy) / `auto b = std::move(a);` (transfer) |
| `copy.deepcopy(a)` | `auto b = a;` |
| returning a list is free | returning a vector is free (elision or move) |
| `a` still usable after `b = a` | `a` is valid but unspecified after a move |
| no ownership concept | move-only types model one owner |
