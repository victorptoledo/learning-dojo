---
from: csharp
status: draft
concept: "Immutability by default"
from_way: "Variables are mutable; readonly fields, const and records with init give immutability."
native_way: "let is immutable by default and mut opts in; shadowing replaces a name."
sensei_tip: "C#'s readonly only protects the field, not the object it points to. Rust's immutability is transitive: through an immutable binding nothing inside can change."
---

| C# | Rust |
| --- | --- |
| var x = 5; | let x = 5; |
| int x = 5; x++; | let mut x = 5; x += 1; |
| readonly field | a field of a non-mut binding |
| const int Max = 10; | const MAX: u32 = 10; |
