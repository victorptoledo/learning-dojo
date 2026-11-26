---
from: go
status: draft
concept: "Iterators, operators and defined types"
from_way: "`range` over built-ins (and `iter.Seq` since 1.23), no operator overloading, and defined types (`type Meters float64`)."
native_way: "`Iterator` and `IntoIterator` traits, operator traits, and newtypes."
sensei_tip: "Go's `type Meters float64` keeps the arithmetic operators; a Rust `struct Meters(f64)` has none until you implement them, so you decide which make sense."
---

| Go | Rust |
| --- | --- |
| `for x := range seq` | `for x in &collection` |
| `iter.Seq[T]` | `impl Iterator<Item = T>` |
| `a.Add(b)` method | `a + b` with `impl Add` |
| `type Meters float64` | `struct Meters(f64);` |
