---
from: java
status: draft
concept: "Polymorphism"
from_way: "Every non-final method is virtual; interfaces, abstract classes, sealed interfaces with switch pattern matching (Java 21)."
native_way: "Only virtual functions are dynamic; base classes need virtual destructors; std::variant with std::visit replaces sealed hierarchies; type erasure gives value semantics."
sensei_tip: "A Java sealed interface with records and a switch maps to std::variant of structs and std::visit: exhaustive, and with no heap allocation."
---

| Java | C++ |
| --- | --- |
| methods virtual by default | only `virtual` functions dispatch dynamically |
| `@Override` | `override` |
| `final class` | `final` |
| `interface Shape` | an abstract class with pure virtual functions (or a concept) |
| `sealed interface Event permits A, B` | `std::variant<A, B>` |
| `switch (e) { case A a -> ... }` | `std::visit(overloaded{...}, e)` |
| `Function<A, B>` | `std::function<B(A)>` |
