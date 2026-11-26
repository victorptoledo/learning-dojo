---
from: csharp
status: draft
concept: "Greedy algorithms"
from_way: "Using OrderBy on a long LINQ chain inside a loop."
native_way: "`Array.Sort(intervals, (a, b) => a[1].CompareTo(b[1]))` once, then one loop; PriorityQueue<int, int> (element and priority both the end time) holds the active meetings."
sensei_tip: "Remember that Array.Sort is not stable in .NET; if ties must keep their input order, add the index as a secondary key or use OrderBy, which is stable."
---

| C# | Java |
| --- | --- |
| `Array.Sort(iv, (a, b) => a[1].CompareTo(b[1]))` | `Arrays.sort(iv, comparingInt(t -> t[1]))` |
| `PriorityQueue<int, int>` | `PriorityQueue<Integer>` |
| `OrderBy(t => t[1])` (stable) | `List.sort` (stable) |
