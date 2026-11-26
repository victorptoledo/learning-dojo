---
from: java
status: draft
concept: "Operators, iterators and wrapper types"
from_way: "`Iterable<T>` with generics, no operator overloading (`a.add(b)`), and wrapper classes or records for type safety."
native_way: "Associated types (`type Item`), the `std::ops` traits for operators, and zero-cost newtypes."
sensei_tip: "A Java record `UserId(long value)` is a heap object; Rust's `struct UserId(u64)` is just a `u64` the compiler will not mix up."
---

| Java | Rust |
| --- | --- |
| `implements Iterable<T>` | `impl IntoIterator for &T` with `type Item` |
| `Iterator<T>.next()` | `Iterator::next(&mut self) -> Option<Self::Item>` |
| `money.add(other)` | `money + other` via `impl Add` |
| `list.get(i)` | `list[i]` via `impl Index<usize>` |
| `record Meters(double v)` | `struct Meters(f64);` |
