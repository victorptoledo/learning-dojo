---
from: go
status: draft
concept: "Interfaces vs traits"
from_way: "Interfaces satisfied implicitly, and generics with type constraints since Go 1.18."
native_way: "Traits implemented explicitly, generics with trait bounds, and dyn for interface-like values."
sensei_tip: "A Go interface value is a Rust dyn Trait. Go generics and Rust generics both use constraints, but Rust compiles a copy per type, so there is no hidden dictionary passing."
---

| Go | Rust |
| --- | --- |
| type Shape interface { Area() float64 } | trait Shape { fn area(&self) -> f64; } |
| func f[T Shape](s T) | fn f<T: Shape>(s: &T) |
| []Shape | Vec<Box<dyn Shape>> |
| String() string (Stringer) | impl Display |
