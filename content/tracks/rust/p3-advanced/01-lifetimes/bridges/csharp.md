---
from: csharp
status: draft
concept: "Object lifetime"
from_way: "The GC tracks every reference; ref struct and Span<T> have scope rules close to lifetimes."
native_way: "Lifetimes for every reference, checked by the borrow checker."
sensei_tip: "If you have used Span<T> and seen error CS8352 (cannot use a ref local outside its scope), you have met lifetimes: Rust applies the same idea to every reference."
---

| C# | Rust |
| --- | --- |
| Span<T> and ref struct rules | lifetimes on every reference |
| scoped keyword | elided lifetimes |
| WeakReference<T> | Weak<T> |
| return a slice of a parameter | fn f<'a>(s: &'a [T]) -> &'a [T] |
