---
from: go
status: draft
concept: "Values and references"
from_way: "Everything is a value, but slices, maps and pointers share their backing data; zero values initialise everything; strings are immutable bytes."
native_way: "Everything is a value too, but standard containers copy their contents deeply; there are no zero values for locals; references alias without pointer syntax."
sensei_tip: "Go's 'every variable has a zero value' does not hold: an uninitialised local int is garbage. Write `int n{};` or initialise it."
---

| Go | C++ |
| --- | --- |
| `b := a` on a slice (shares) | `auto b = a;` on a vector (deep copy) |
| zero values | only for `{}`-initialised or global variables |
| `*int` | `int*` or `int&` |
| `string` (immutable bytes) | `std::string` (mutable bytes) |
| substring shares bytes | `std::string_view` shares; `substr` copies |
| overflow wraps | unsigned wraps, signed overflow is UB |
