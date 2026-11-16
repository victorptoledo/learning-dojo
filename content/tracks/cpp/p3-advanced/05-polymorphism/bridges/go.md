---
from: go
status: draft
concept: "Polymorphism"
from_way: "Implicit interfaces, type switches, and generics; no inheritance."
native_way: "Virtual functions with explicit base classes, std::variant with std::visit like a checked type switch, concepts like Go's implicit interfaces at compile time."
sensei_tip: "A Go interface value is type erasure; std::function and hand-written type-erased wrappers are the C++ version, and concepts are the compile-time version."
---

| Go | C++ |
| --- | --- |
| `type Shape interface { Area() float64 }` | a concept (compile time) or an abstract class (run time) |
| implicit satisfaction | concepts are implicit; base classes are explicit |
| type switch `switch v := x.(type)` | `std::visit` over a `std::variant` |
| embedding for reuse | composition (or CRTP) |
| `func(int) int` values | `std::function<int(int)>` |
| `any` | `std::any` |
