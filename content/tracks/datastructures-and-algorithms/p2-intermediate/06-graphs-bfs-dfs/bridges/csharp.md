---
from: csharp
status: draft
concept: "Graph traversal"
from_way: "Navigating Entity Framework relationships recursively and loading the whole object graph."
native_way: "Dictionary<T, List<T>> as the adjacency list, Queue<T> for BFS, Stack<T> for DFS and HashSet<T> for visited. MSBuild and the dependency injection container in Microsoft.Extensions.DependencyInjection both work with dependency graphs and report cycles."
sensei_tip: "`HashSet<T>.Add` returns false when the item was already there, so `if (seen.Add(next)) queue.Enqueue(next);` marks and enqueues in one step; Java's `Set.add` behaves the same way."
---

| C# | Java |
| --- | --- |
| `Dictionary<T, List<T>>` | `Map<T, List<T>>` |
| `Queue<T>` | `ArrayDeque` as a queue |
| `Stack<T>` | `ArrayDeque` as a stack |
| `if (seen.Add(x))` | `if (seen.add(x))` |
| `PriorityQueue<T, string>` for Kahn | `PriorityQueue<String>` |
