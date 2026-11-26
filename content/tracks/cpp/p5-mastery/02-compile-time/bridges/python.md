---
from: python
status: draft
concept: "Compile-time programming"
from_way: "Everything happens at run time; module-level code runs at import."
native_way: "constexpr functions and static_assert run in the compiler; results are baked into the binary."
sensei_tip: "Code you would run at import time to build a lookup table can be a constexpr function in C++, costing nothing at start-up."
---

| Python | C++ |
| --- | --- |
| module-level table built at import | `constexpr auto table = [] { ... }();` |
| `assert` in tests | `static_assert` |
| `isinstance` / `type()` | type traits, `if constexpr` |
| metaclasses, decorators | templates (and C++26 reflection) |
| Final constants (typing) | `constexpr` |
