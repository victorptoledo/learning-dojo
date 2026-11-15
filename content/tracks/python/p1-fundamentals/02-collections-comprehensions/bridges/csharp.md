---
from: csharp
status: draft
concept: "Collections and LINQ"
from_way: "List<T>, Dictionary<TKey, TValue> and HashSet<T>, LINQ with Where, Select, GroupBy and OrderBy().ThenBy(), and value tuples as keys."
native_way: "list, dict and set literals, comprehensions for Where and Select, Counter and defaultdict for GroupBy, and sorted with tuple keys."
sensei_tip: "LINQ's deferred execution maps to generator expressions; a comprehension in square brackets is ToList() and runs at once."
---

| C# | Python |
| --- | --- |
| `xs.Where(p).Select(f).ToList()` | `[f(x) for x in xs if p(x)]` |
| `xs.Select(f)` (deferred) | `(f(x) for x in xs)` |
| `dict.TryGetValue(k, out var v)` | `v = d.get(k)` |
| `xs.GroupBy(f).ToDictionary(g => g.Key, g => g.Count())` | `Counter(map(f, xs))` |
| `OrderByDescending(x => x.N).ThenBy(x => x.Name)` | `sorted(xs, key=lambda x: (-x.N, x.Name))` |
| `(int, int)` as a dictionary key | `tuple[int, int]` as a dict key |
| `Queue<T>` / `LinkedList<T>` | `collections.deque` |
