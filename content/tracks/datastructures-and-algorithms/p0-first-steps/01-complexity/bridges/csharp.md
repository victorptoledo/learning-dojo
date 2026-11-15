---
from: csharp
status: draft
concept: "Cost of operations and amortised growth"
from_way: "Chaining LINQ calls such as `list.Where(x => other.Contains(x))` without noticing that `Contains` on a `List<T>` scans it."
native_way: "`List<T>` is a dynamic array that doubles its capacity, so `Add` is amortised O(1), but `Contains`, `IndexOf`, `Insert(0, x)` and `RemoveAt(0)` are O(n). `HashSet<T>` and `Dictionary<K,V>` are hash tables."
sensei_tip: "The .NET docs state the cost of most collection methods in their Remarks section (\"This method is an O(n) operation\"). Pass a capacity to `new List<T>(n)` or `new Dictionary<K,V>(n)` when you know the size."
---

| C# | Java | Cost |
| --- | --- | --- |
| `list.Add(x)` | `list.add(x)` | amortised O(1) (doubles) |
| `list[i]` | `list.get(i)` | O(1) |
| `list.Insert(0, x)` / `RemoveAt(0)` | `list.add(0, x)` / `remove(0)` | O(n) |
| `list.Contains(x)` | `list.contains(x)` | O(n) |
| `HashSet<T>.Contains` / `Dictionary.TryGetValue` | `HashSet.contains` / `HashMap.get` | O(1) expected |
| `list.Sort()` | `List.sort` | O(n log n), introsort, not stable |
