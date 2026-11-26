---
from: csharp
status: draft
concept: "IEnumerable, yield return and LINQ"
from_way: "IEnumerable<T> and IEnumerator<T>, iterator methods with yield return and yield break, deferred LINQ operators, Dispose running finally."
native_way: "Iterable<T> and Iterator<T>, generator functions with yield and return, lazy iterator helpers, return() running finally."
sensei_tip: "yield return maps directly to yield, and deferred execution works the same way. The catch: there is no LINQ on arrays that is lazy; use Iterator.from(array) or array.values() to get the lazy helpers."
---

| C# | TypeScript |
| --- | --- |
| `IEnumerable<T>` | `Iterable<T>` |
| `yield return x;` | `yield x;` |
| `yield break;` | `return;` |
| `xs.Where(p).Select(f).Take(5)` | `Iterator.from(xs).filter(p).map(f).take(5)` |
| `.ToList()` | `.toArray()` |
| `foreach` calls `Dispose()` early | `for...of` calls `return()` early |
| `GetEnumerator()` | `[Symbol.iterator]()` |
