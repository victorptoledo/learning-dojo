---
from: csharp
status: draft
concept: "Ranges and LINQ"
from_way: "LINQ with deferred execution, ToList() to materialise, yield return for iterators."
native_way: "Views are deferred like LINQ, ranges::to materialises them, std::generator with co_yield replaces yield return."
sensei_tip: "LINQ method syntax maps almost one to one onto view adaptors joined with |."
---

| C# | C++ |
| --- | --- |
| `Where(p)` / `Select(f)` | `views::filter(p)` / `views::transform(f)` |
| `ToList()` | `ranges::to<std::vector>()` |
| `Take(n)` / `Skip(n)` | `views::take(n)` / `views::drop(n)` |
| `yield return x;` | `co_yield x;` |
| `OrderBy(x => x.Age)` | `ranges::sort(v, {}, &T::age)` |
| `Zip`, `Chunk` | `views::zip`, `views::chunk` |
