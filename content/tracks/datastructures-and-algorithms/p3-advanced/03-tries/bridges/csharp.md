---
from: csharp
status: draft
concept: "Tries and prefix search"
from_way: "Using LINQ Where(s => s.StartsWith(prefix)) over the whole list."
native_way: "A Node class with Dictionary<char, Node> children (or an array for small alphabets). ASP.NET Core's endpoint routing builds a decision tree over URL segments, a close relative of a trie."
sensei_tip: "Pass `StringComparison.Ordinal` to `StartsWith` when you do filter strings: the default culture-sensitive comparison is much slower and can give surprising matches."
---

| C# | Java |
| --- | --- |
| `Dictionary<char, Node>` children | `HashMap<Character, Node>` |
| `Node[] children = new Node[26]` | same |
| `s.StartsWith(p, StringComparison.Ordinal)` | `s.startsWith(p)` |
| `SortedSet<string>.GetViewBetween(p, p + '￿')` | `TreeSet.subSet` |
| `PriorityQueue<string, long>` for top k | `PriorityQueue` |
