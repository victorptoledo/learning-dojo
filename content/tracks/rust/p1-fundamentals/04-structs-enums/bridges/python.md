---
from: python
status: draft
concept: "Data modelling and polymorphism"
from_way: "Classes or dataclasses, Enum for constants, and isinstance or match for variants."
native_way: "Structs for data, enums whose variants carry data, and exhaustive match."
sensei_tip: "Python 3.10's match looks similar, but Rust's match must cover every variant, so adding one breaks the build where it is not handled yet."
---

| Python | Rust |
| --- | --- |
| @dataclass class Point | struct Point { x: f64, y: f64 } |
| class Color(Enum) | enum Color { Red, Green } |
| Union[Circle, Square] | enum Shape { Circle(f64), Square(f64) } |
| match shape: case Circle(r=r): | match shape { Shape::Circle(r) => ... } |
| def area(self) | impl Shape { fn area(&self) -> f64 } |
