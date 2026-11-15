---
from: csharp
status: draft
concept: "Specification"
from_way: "Repository methods per filter combination and LINQ conditions copied across services."
native_way: "`Expression<Func<T, bool>>` specifications that EF Core translates into SQL and that can also be compiled and run in memory."
sensei_tip: "C# has the best built-in support: an `Expression<Func<Customer, bool>>` is a specification as data, so the same rule runs with `.Compile()` in memory and translates to SQL through LINQ to Entities."
---

| C# | Specification idea |
| --- | --- |
| `Func<T, bool>` | rule as a function |
| `Expression<Func<T, bool>>` | rule as data |
| `query.Where(spec)` in EF Core | interpreted into SQL |
| `spec.Compile()(customer)` | interpreted in memory |
| Ardalis.Specification | a ready-made specification library |
