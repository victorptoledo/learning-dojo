---
from: javascript
status: draft
concept: "Interfaces vs traits"
from_way: "Structural interfaces in TypeScript and generics erased at runtime."
native_way: "Traits implemented explicitly, with static or dynamic dispatch chosen by you."
sensei_tip: "TypeScript interfaces match by shape; Rust traits must be implemented with impl. In exchange you can add a trait to someone else's type, like Display for your struct."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| interface Shape { area(): number } | trait Shape { fn area(&self) -> f64; } |
| function f<T extends Shape>(s: T) | fn f<T: Shape>(s: &T) |
| Shape[] | Vec<Box<dyn Shape>> |
| toString() | impl Display |
