---
from: csharp
status: draft
concept: "Design patterns"
from_way: "The Java 1.4 shapes in C#: an interface and a class per strategy, an abstract visitor, a lazy singleton with double-checked locking."
native_way: "Delegates and lambdas for strategies and commands, events for observer, records for values, switch expressions with patterns for closed hierarchies, and the built-in DI container for creation."
sensei_tip: "`IEnumerable` with `yield return` is iterator, `event` is observer, and `Lazy<T>` gives you a thread-safe lazy singleton without writing the locking yourself."
---

| C# | Pattern |
| --- | --- |
| `Func<T, TResult>`, `Action<T>` | strategy, command |
| `event EventHandler<T>` | observer |
| `IEnumerable<T>` and `yield return` | iterator |
| `record` and `with` expressions | value objects, prototype |
| `switch` expressions with type patterns | visitor |
| `services.AddSingleton<T>()` | singleton by the container |
