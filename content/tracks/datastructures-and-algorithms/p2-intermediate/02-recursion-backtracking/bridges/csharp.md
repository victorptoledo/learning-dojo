---
from: csharp
status: draft
concept: "Recursion and backtracking"
from_way: "Catching StackOverflowException to recover from deep recursion."
native_way: "In .NET a stack overflow cannot be caught: the process is terminated. So deep recursion must be turned into a loop with a Stack<T> before it ships. The 1 MB default thread stack is similar to Java's."
sensei_tip: "`yield return` inside a recursive iterator method enumerates backtracking results lazily, and `RuntimeHelpers.EnsureSufficientExecutionStack()` throws a catchable exception before the real overflow, which compilers like Roslyn use for deeply nested input."
---

| C# | Java |
| --- | --- |
| stack overflow kills the process | `StackOverflowError` can be caught (but should not be relied on) |
| `Stack<T>` explicit stack | `ArrayDeque` |
| `path.Add(x)` / `path.RemoveAt(path.Count - 1)` | `path.add(x)` / `path.removeLast()` |
| `out.Add(path.ToList())` | `out.add(List.copyOf(path))` |
| `yield return` | an `Iterator` |
| `Parallel.Invoke` | `ForkJoinPool` |
