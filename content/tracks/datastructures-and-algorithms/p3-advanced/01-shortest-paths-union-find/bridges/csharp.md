---
from: csharp
status: draft
concept: "Weighted graphs and connectivity"
from_way: "Implementing a heap by hand because older .NET had none."
native_way: "PriorityQueue<int, long> (since .NET 6) holds the node as the element and the distance as the priority, which reads naturally for Dijkstra; lazy deletion works the same as in Java. Union-find is two int arrays."
sensei_tip: "`queue.Enqueue(node, distance)` and `queue.TryDequeue(out var node, out var d)` give you both values without a tuple. Like Java's PriorityQueue, it has no decrease-key, so skip outdated entries."
---

| C# | Java |
| --- | --- |
| `PriorityQueue<int, long>` | `PriorityQueue<long[]>` |
| `Enqueue(node, dist)` | `add(new long[] {dist, node})` |
| `TryDequeue(out node, out d)` | `poll()` |
| `long.MaxValue` | `Long.MAX_VALUE` |
| `int[] parent, size` | `int[] parent, size` |
| `Array.Sort(edges, (a, b) => a[2].CompareTo(b[2]))` | `Arrays.sort(edges, comparingInt(e -> e[2]))` |
