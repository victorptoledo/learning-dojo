---
from: csharp
status: draft
concept: "Operators, enumerators and wrappers"
from_way: "`IEnumerable<T>` with `yield return`, `operator +` overloads, and `record struct` wrappers."
native_way: "`Iterator` with an associated `Item`, the `std::ops` traits, and newtypes."
sensei_tip: "C# `operator +` is a static method; Rust's `impl Add` is a trait, so generic code can require it: `T: Add<Output = T>`."
---

| C# | Rust |
| --- | --- |
| `IEnumerable<T>` / `GetEnumerator` | `IntoIterator` / `Iterator` |
| `public static Money operator +(...)` | `impl Add for Money` |
| indexer `this[int i]` | `impl Index<usize>` |
| `readonly record struct UserId(long V)` | `struct UserId(u64);` |
| `implicit operator` | `impl From<A> for B` (always explicit at the call: `.into()`) |
