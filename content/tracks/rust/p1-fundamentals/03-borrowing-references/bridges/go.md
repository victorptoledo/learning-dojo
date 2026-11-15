---
from: go
status: draft
concept: "Pass by reference vs aliasing"
from_way: "Pointers and slices that share their backing array; races are found by go test -race."
native_way: "References checked at compile time, so aliasing and data races cannot compile."
sensei_tip: "Appending to a Go slice while another slice shares its array is a classic bug. Rust's rule of one &mut or many & makes it impossible."
---

| Go | Rust |
| --- | --- |
| func f(p *T) | fn f(p: &mut T) |
| func f(s []int) (read only by convention) | fn f(s: &[i32]) |
| go test -race | the borrow checker |
| &x | &x or &mut x |
