---
from: csharp
status: draft
concept: "Two pointers and sliding windows"
from_way: "Using LINQ Skip/Take/Sum for every window, which rescans and allocates."
native_way: "Two int indexes, a running aggregate and a Dictionary or a small array, exactly as in Java. Span<T> lets you look at a window without copying when you do need the slice."
sensei_tip: "`CollectionsMarshal.GetValueRefOrAddDefault(dict, key, out _)++` increments a count with one lookup. `dict.GetValueOrDefault(prefix - k)` mirrors Java's getOrDefault."
---

| Pattern | C# | Java |
| --- | --- | --- |
| opposite ends | `int i = 0, j = a.Length - 1` | same |
| window counts | `Dictionary<char, int>` / `int[128]` | `HashMap` / `int[128]` |
| window max | `LinkedList<int>` or a ring buffer | `ArrayDeque` |
| slice without copying | `a.AsSpan(left, right - left + 1)` | index range |
| prefix sums | `long[] prefix` | `long[] prefix` |
