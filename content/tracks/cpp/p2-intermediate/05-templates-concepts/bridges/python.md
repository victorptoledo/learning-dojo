---
from: python
status: draft
concept: "Generic code"
from_way: "Duck typing at run time; typing.TypeVar and Protocol for static checkers only."
native_way: "Templates are duck typing checked at compile time; concepts are like Protocols that the compiler enforces."
sensei_tip: "A typing.Protocol you used for mypy becomes a concept the compiler enforces at every call."
---

| Python | C++ |
| --- | --- |
| duck typing (run time) | templates (compile time) |
| `TypeVar("T", bound=SupportsLt)` | `template <std::totally_ordered T>` |
| `Protocol` with `area()` | `concept Shape = requires(const T& s) { s.area(); };` |
| `isinstance` checks | `if constexpr (std::integral<T>)` |
| `*args` | variadic templates `Ts... args` |
| mypy error | compile error at the call site |
