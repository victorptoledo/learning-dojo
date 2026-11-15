---
from: python
status: draft
concept: "Interfaces vs traits"
from_way: "Duck typing and Protocols, with generics only as type hints."
native_way: "Traits that types implement explicitly, and generics checked and compiled per type."
sensei_tip: "A typing.Protocol is a trait checked by mypy; a Rust trait bound is checked by the compiler and costs nothing at runtime, because generic code is compiled for each type."
---

| Python | Rust |
| --- | --- |
| class Shape(Protocol) | trait Shape |
| def area(s: Shape) | fn area(s: &impl Shape) |
| list[Shape] of mixed types | Vec<Box<dyn Shape>> |
| __str__ | impl Display |
| TypeVar T | <T: Trait> |
