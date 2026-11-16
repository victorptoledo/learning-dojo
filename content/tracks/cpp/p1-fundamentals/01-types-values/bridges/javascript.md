---
from: javascript
status: draft
concept: "Values and references"
from_way: "Primitives (number, string) are values, objects and arrays are references; number is a 64-bit float; strings are immutable UTF-16."
native_way: "Objects are values too: a std::vector is copied on assignment; integers and floating point are separate types; std::string holds bytes."
sensei_tip: "Assume nothing is shared unless you made it a reference. And prefer int or std::int64_t for counts: double is not your default number anymore."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `const arr2 = arr` (same array) | `auto v2 = v;` (a copy) |
| `number` | `int`, `std::int64_t`, `double`... |
| `const x = ...` (binding only) | `const auto x = ...` (the object cannot change) |
| string (UTF-16) | `std::string` (bytes, usually UTF-8) |
| `undefined` / `null` | `nullptr`, `std::optional` |
| `Number.MAX_SAFE_INTEGER` | `std::numeric_limits<int>::max()`; overflow is UB |
