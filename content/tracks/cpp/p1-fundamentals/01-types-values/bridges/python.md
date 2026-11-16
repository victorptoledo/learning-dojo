---
from: python
status: draft
concept: "Values and names"
from_way: "Names are references to objects; ints never overflow; str is immutable; assignment never copies."
native_way: "Variables are objects with a fixed type; integers have fixed widths and can overflow; assignment copies, references (&) alias."
sensei_tip: "Write the type you mean (or auto with an initialiser) and initialise every variable: C++ will not invent a value for you."
---

| Python | C++ |
| --- | --- |
| `b = a` (same list) | `auto b = a;` (copy of the vector) |
| unbounded `int` | `int` 32-bit, `long long` 64-bit; overflow is UB for signed |
| `str` immutable | `std::string` mutable; `std::string_view` read-only window |
| `len(s)` characters | `s.size()` bytes |
| `None` | `nullptr` for pointers, `std::optional` for values |
| dynamic types | static types checked at compile time |
