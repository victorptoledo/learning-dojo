---
from: python
status: draft
concept: "Functions and parameters"
from_way: "def functions take names bound to objects; return a tuple for several values; None for no result; lambdas are one expression."
native_way: "Each parameter has a type and a passing mode; return a struct and unpack it with structured bindings; std::optional for no result; lambdas are full functions."
sensei_tip: "Python's 'mutating a list argument changes the caller's list' only happens in C++ when the parameter is a reference."
---

| Python | C++ |
| --- | --- |
| `def area(w, h):` | `int area(int w, int h)` |
| `return lo, hi` then `lo, hi = f()` | `return {lo, hi};` then `auto [lo, hi] = f();` |
| `None` | `std::nullopt` |
| `lambda x: x * 2` | `[](int x) { return x * 2; }` |
| default arguments | default arguments (in the declaration) |
| `enum.Enum` | `enum class` |
| closures see later changes | `[x]` captures a copy now; `[&x]` sees changes |
