---
from: python
status: draft
concept: "Polymorphism"
from_way: "Duck typing, abc.ABC with abstractmethod, Protocols, and match statements on classes."
native_way: "Virtual functions for open hierarchies, std::variant and std::visit for closed sets, concepts for compile-time duck typing, type erasure for callables."
sensei_tip: "A `match` statement over a fixed set of dataclasses is a std::visit over a std::variant, with the compiler checking every case."
---

| Python | C++ |
| --- | --- |
| duck typing | templates + concepts (compile time) |
| `class Shape(ABC)` + `@abstractmethod` | `virtual double area() const = 0;` |
| overriding a method | `override` on a virtual function |
| `match event: case Deposit(...)` | `std::visit` over `std::variant` |
| callables everywhere | `std::function` / `std::move_only_function` |
| `Any` | `std::any` (rarely needed) |
