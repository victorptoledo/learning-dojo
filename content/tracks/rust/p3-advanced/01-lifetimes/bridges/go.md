---
from: go
status: draft
concept: "Object lifetime"
from_way: "Escape analysis moves values to the heap whenever a pointer outlives its function."
native_way: "References must not outlive their values; owned values or Box move data to the heap explicitly."
sensei_tip: "Go silently heap-allocates when you return &local; Rust refuses and asks you to return the value itself, or a Box. The cost is visible in the code."
---

| Go | Rust |
| --- | --- |
| return &local | return the value or a Box<T> |
| pointer stored in a struct | struct S<'a> { r: &'a T } |
| GC | ownership and lifetimes |
| sub-slice of an argument | fn f<'a>(s: &'a [T]) -> &'a [T] |
