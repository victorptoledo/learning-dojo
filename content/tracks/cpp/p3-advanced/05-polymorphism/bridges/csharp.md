---
from: csharp
status: draft
concept: "Polymorphism"
from_way: "virtual and override, abstract classes, interfaces, pattern matching with switch expressions, delegates."
native_way: "virtual and override (plus a virtual destructor), abstract classes, std::variant with std::visit, std::function, and concepts for static interfaces."
sensei_tip: "Keywords match C# closely; the new rules are the virtual destructor and never storing derived objects by value in a base-typed container."
---

| C# | C++ |
| --- | --- |
| `virtual` / `override` / `sealed` | `virtual` / `override` / `final` |
| `abstract class Shape` | a class with `= 0` functions |
| `interface IShape` | an abstract class or a concept |
| `switch` expression over types | `std::visit` over `std::variant` |
| `Func<T>` / delegates | `std::function<T()>` |
| `List<Shape>` holds references | `std::vector<std::unique_ptr<Shape>>` |
