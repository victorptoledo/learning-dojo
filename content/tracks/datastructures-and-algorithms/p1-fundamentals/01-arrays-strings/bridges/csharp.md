---
from: csharp
status: draft
concept: "Arrays and strings"
from_way: "Using List<T> and string concatenation everywhere, and Substring in loops."
native_way: "Arrays of value types (`int[]`) are contiguous like Java's; `List<T>` of a value type also stores values without boxing, unlike Java. `Span<T>` and `ReadOnlySpan<char>` give views into arrays and strings without copying."
sensei_tip: "`s.AsSpan(i, j - i)` is a substring with no allocation, and `StringBuilder` or `string.Create` builds text efficiently. Java's closest equivalents are `CharBuffer.wrap` and passing (array, from, to) index triples."
---

| C# | Java | Note |
| --- | --- | --- |
| `int[]` | `int[]` | contiguous values |
| `List<int>` | `ArrayList<Integer>` | C# stores ints unboxed; Java boxes |
| `Span<T>` / `ReadOnlySpan<char>` | index ranges, `CharBuffer` | views without copying |
| `string` (immutable UTF-16) | `String` | same model |
| `s.Substring(i, n)` | `s.substring(i, i + n)` | copies |
| `StringBuilder` | `StringBuilder` | amortised O(1) append |
| `string.Equals(a, b, StringComparison.Ordinal)` | `a.equals(b)` | ordinal comparison |
