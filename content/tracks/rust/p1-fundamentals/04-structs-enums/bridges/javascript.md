---
from: javascript
status: draft
concept: "Data modelling and polymorphism"
from_way: "Objects, classes and TypeScript discriminated unions with a kind field."
native_way: "Structs and enums with data, matched exhaustively."
sensei_tip: "A TypeScript union { kind: 'circle', r } | { kind: 'square', side } is a Rust enum written by hand. Rust's version needs no kind field and match checks exhaustiveness without tricks."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| interface Point { x: number; y: number } | struct Point { x: f64, y: f64 } |
| type Shape = Circle \| Square | enum Shape { Circle(f64), Square(f64) } |
| switch (s.kind) | match s |
| class methods | impl blocks |
