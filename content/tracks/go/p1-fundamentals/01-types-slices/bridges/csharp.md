---
from: csharp
status: draft
concept: "Value types, arrays, lists and dictionaries"
from_way: "Value and reference types, default(T), List<T> and Dictionary<TKey, TValue>, Span<T> for views over arrays, and implicit numeric widening."
native_way: "Zero values for every type, slices (a built-in view with a length and capacity) and maps in the language, and explicit conversions everywhere."
sensei_tip: "A Go slice behaves like Span<T> that can also grow: cheap to pass, a view over shared memory, and reallocated by append when it runs out of room."
---

| C# | Go |
| --- | --- |
| `default(int)` | the zero value `0` |
| `var list = new List<string>();` | `var list []string` |
| `list.Add("a")` | `list = append(list, "a")` |
| `arr.AsSpan(1, 2)` | `arr[1:3]` |
| `new Dictionary<string, int>()` | `map[string]int{}` |
| `dict.TryGetValue(k, out var v)` | `v, ok := dict[k]` |
| `long y = x;` | `y := int64(x)` |
| `foreach (var (k, v) in dict)` | `for k, v := range dict` (random order) |
