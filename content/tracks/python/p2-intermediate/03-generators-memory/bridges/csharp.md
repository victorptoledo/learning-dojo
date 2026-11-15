---
from: csharp
status: draft
concept: "Iterators and deferred execution"
from_way: "IEnumerable and IEnumerator, yield return and yield break, LINQ's deferred execution, Take, Concat and GroupBy, and argument checks moved out of iterator methods to fail early."
native_way: "__iter__ and __next__, yield and return, generator expressions, itertools.islice, chain and groupby, and the same early-validation pattern with a wrapper function."
sensei_tip: "C# developers already know the deferred-error trap of yield return; Python has the same one, and the same fix: validate in a normal function and return the generator from a private one."
---

| C# | Python |
| --- | --- |
| `IEnumerable<T>` method with `yield return` | a function with `yield` |
| `yield break` | `return` |
| `xs.Where(p).Select(f)` (deferred) | `(f(x) for x in xs if p(x))` |
| `xs.Take(5)` / `xs.Skip(2)` | `islice(xs, 5)` / `islice(xs, 2, None)` |
| `a.Concat(b)` | `chain(a, b)` |
| `xs.Chunk(500)` | `batched(xs, 500)` |
| `File.ReadLines(path)` | `for line in path.open():` |
