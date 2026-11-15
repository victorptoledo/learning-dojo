---
from: go
status: draft
concept: "Immutability by default"
from_way: "Variables are mutable, := declares, and const exists only for compile-time values."
native_way: "Immutable by default, with mut where change is needed."
sensei_tip: "Go has no immutable variables at all, so it relies on discipline. In Rust the compiler tells you when a value is changed, which makes reading other people's code easier."
---

| Go | Rust |
| --- | --- |
| x := 5 | let x = 5; |
| x := 5; x++ | let mut x = 5; x += 1; |
| x := strings.TrimSpace(x) | let x = x.trim(); |
| const Max = 10 | const MAX: u32 = 10; |
