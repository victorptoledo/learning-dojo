---
from: csharp
status: draft
concept: "Interfaces vs traits"
from_way: "Interfaces, generics with where constraints, and virtual dispatch."
native_way: "Traits with generics that are monomorphised, and dyn Trait when you want dynamic dispatch."
sensei_tip: "where T : IShape is <T: Shape>. Rust's generics are always specialised per type, like C# generics over structs, so there is no boxing and no virtual call unless you ask for dyn."
---

| C# | Rust |
| --- | --- |
| interface IShape | trait Shape |
| void F<T>(T s) where T : IShape | fn f<T: Shape>(s: &T) |
| List<IShape> | Vec<Box<dyn Shape>> |
| ToString() | impl Display |
| IComparable<T> | impl Ord |
