---
from: csharp
status: draft
concept: "Dynamic programming"
from_way: "Using jagged arrays (int[][]) everywhere and Dictionary<string, long> memos."
native_way: "C# has true rectangular arrays (`new int[n + 1, m + 1]`), which are one block of memory, as well as jagged arrays; `stackalloc` or `Span<int>` gives rolling rows with no heap allocation. Value tuples `(int, int)` make cheap dictionary keys."
sensei_tip: "`Dictionary<(int i, int j), long>` is a clean sparse memo, and `checked { ... }` makes overflow throw instead of wrapping when a count grows too large."
---

| C# | Java |
| --- | --- |
| `new long[n + 1]` | `new long[n + 1]` |
| `new int[n + 1, m + 1]` (rectangular) | `new int[n + 1][m + 1]` (array of arrays) |
| `Span<int> row = stackalloc int[m + 1]` | two `int[]` rows |
| `Dictionary<(int, int), long>` | `HashMap<record, Long>` |
| `checked { }` | `Math.addExact` |
