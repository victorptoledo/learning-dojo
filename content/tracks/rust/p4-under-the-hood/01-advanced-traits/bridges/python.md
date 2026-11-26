---
from: python
status: draft
concept: "Dunder methods"
from_way: "`__iter__`, `__add__`, `__getitem__` and `__len__` on any class, checked at run time."
native_way: "The same hooks as traits (`IntoIterator`, `Add`, `Index`), checked at compile time."
sensei_tip: "If you know `__add__`, you know `impl Add`; the difference is that Rust picks the impl at compile time and `Output` is declared."
---

| Python | Rust |
| --- | --- |
| `__iter__` / `__next__` | `IntoIterator` / `Iterator::next` |
| `__add__`, `__neg__` | `Add`, `Neg` |
| `__getitem__` | `Index`, `IndexMut` |
| `typing.NewType('UserId', int)` | `struct UserId(u64);` enforced by the compiler |
| `__eq__`, `__lt__` | `#[derive(PartialEq, PartialOrd)]` |
