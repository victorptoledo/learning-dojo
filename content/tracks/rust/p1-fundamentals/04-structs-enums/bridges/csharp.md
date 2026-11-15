---
from: csharp
status: draft
concept: "Data modelling and polymorphism"
from_way: "Classes and records, enum as named integers, and switch expressions with type patterns."
native_way: "Structs, enums that carry data, and match."
sensei_tip: "A C# enum is just named numbers. A Rust enum is a closed set of types, like a sealed record hierarchy, with match as the switch expression the compiler checks."
---

| C# | Rust |
| --- | --- |
| record Point(double X, double Y) | struct Point { x: f64, y: f64 } |
| enum Color { Red, Green } | enum Color { Red, Green } |
| abstract record Shape + derived records | enum Shape { Circle(f64), Square(f64) } |
| switch expression | match |
