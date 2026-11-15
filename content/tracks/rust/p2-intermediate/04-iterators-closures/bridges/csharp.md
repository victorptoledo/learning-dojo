---
from: csharp
status: draft
concept: "Iterators and closures"
from_way: "LINQ with lambdas, deferred execution and IEnumerable."
native_way: "Iterators with adapters, also lazy, and closures with explicit capture rules."
sensei_tip: "LINQ maps almost directly: Where is filter, Select is map, Aggregate is fold, ToList is collect. Both are lazy; Rust's has no allocations per step and no virtual calls."
---

| C# | Rust |
| --- | --- |
| xs.Where(x => x > 0) | xs.iter().filter(\|x\| **x > 0) |
| Select | map |
| Aggregate | fold |
| ToList() | collect::<Vec<_>>() |
| First(...) | find(...) |
| yield return | impl Iterator |
