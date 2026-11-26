---
from: csharp
status: draft
concept: "Tasks and awaiters"
from_way: "`Task` starts hot; `await` registers a continuation through an awaiter and `SynchronizationContext`."
native_way: "Futures are cold; `.await` compiles into `poll`, and progress is signalled with a `Waker`."
sensei_tip: "C#'s `INotifyCompletion.OnCompleted(continuation)` plays the role of storing the waker; Rust then asks the executor to poll again instead of running the continuation inline."
---

| C# | Rust |
| --- | --- |
| `Task.Run(...)` | spawn on an executor |
| `GetAwaiter().OnCompleted` | store `cx.waker()` |
| `Task.WhenAll` | `join_all` |
| `CancellationToken` | dropping the future |
