---
from: csharp
status: draft
concept: "Coding interviews"
from_way: "Reaching for LINQ chains in interview code and hiding their cost."
native_way: "C# has everything Java has for interviews: Dictionary, HashSet, Queue, Stack, PriorityQueue (with separate priorities), SortedSet and SortedDictionary. LINQ is fine for clarity, but state what it costs (OrderBy is O(n log n), Count() on an IEnumerable may enumerate everything)."
sensei_tip: "`dict.TryGetValue(k, out var v)` and `CollectionsMarshal.GetValueRefOrAddDefault` are the counting idioms; for sorting pairs, value tuples compare element by element: `list.Sort((a, b) => (a.x, a.y).CompareTo((b.x, b.y)))`."
---

| Need | C# | Java |
| --- | --- | --- |
| count | `dict[k] = dict.GetValueOrDefault(k) + 1` | `merge` |
| queue / stack | `Queue<T>` / `Stack<T>` | `ArrayDeque` |
| heap | `PriorityQueue<T, P>` | `PriorityQueue<T>` |
| sorted set | `SortedSet<T>` | `TreeSet<T>` |
| tuple compare | `(a.x, a.y).CompareTo((b.x, b.y))` | chained comparator |
