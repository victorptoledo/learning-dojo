---
from: go
status: draft
concept: "Data modelling and polymorphism"
from_way: "Structs, iota constants, and interfaces with type switches for variants."
native_way: "Structs, data-carrying enums, and exhaustive match."
sensei_tip: "Go's type switch has no exhaustiveness check and iota enums accept any integer. A Rust enum value can only be one of its variants, and match proves you handled each."
---

| Go | Rust |
| --- | --- |
| type Point struct { X, Y float64 } | struct Point { x: f64, y: f64 } |
| const ( Red Color = iota ) | enum Color { Red, Green } |
| interface + type switch | enum + match |
| func (p Point) Dist() | impl Point { fn dist(&self) } |
