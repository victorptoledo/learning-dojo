---
from: java
status: draft
concept: "Moving instead of copying"
from_way: "Objects are references, so passing one never copies it; there is nothing to move."
native_way: "Objects are values, so copies are real; move semantics let a dying object hand over its buffers instead of duplicating them."
sensei_tip: "When a C++ value would be copied and you no longer need the original, std::move it. A Java developer gets reference-like cost with value-like safety."
---

| Java | C++ |
| --- | --- |
| passing a list shares it | passing a vector copies it, unless moved or by reference |
| `return list;` (a reference) | `return v;` (elided or moved, never copied) |
| no equivalent | `std::move(x)` (cast to rvalue) |
| `Collections.unmodifiableList` | `const T&` |
| one owner by convention | move-only types: `std::unique_ptr`, `std::jthread` |
| constructor `this.items = items` | `items_(std::move(items))` |
