---
from: csharp
status: draft
concept: "String matching"
from_way: "`text.IndexOf(p, from)` in a loop, often without `StringComparison.Ordinal`."
native_way: "Pass `StringComparison.Ordinal` for exact matches: the default `IndexOf(string)` is culture-sensitive and much slower. .NET 8 added `SearchValues<string>`, a vectorised multi-pattern search, for the many-patterns case."
sensei_tip: "`text.AsSpan().IndexOf(p.AsSpan())` searches without allocating and is ordinal by default. KMP is still the tool when you need overlapping matches or a guaranteed linear worst case."
---

| C# | Java |
| --- | --- |
| `IndexOf(p, StringComparison.Ordinal)` | `indexOf(p)` |
| `ReadOnlySpan<char>.IndexOf` | `indexOf` on a `String` |
| `SearchValues.Create(patterns, StringComparison.Ordinal)` | Aho-Corasick by hand |
| `Math.BigMul(a, b, out low)` | `Math.multiplyHigh(a, b)` |
| `HashSet<ulong>` of window hashes | `HashSet<Long>` |
