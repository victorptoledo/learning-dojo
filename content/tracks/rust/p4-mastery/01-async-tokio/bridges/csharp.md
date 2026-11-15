---
from: csharp
status: draft
concept: "Async I/O and the event loop"
from_way: "async/await over Task with the thread pool and SynchronizationContext."
native_way: "async/await over futures with an explicit runtime (Tokio)."
sensei_tip: "C#'s Task.Run for blocking work is spawn_blocking; Task.WhenAll is join!. Unlike a Task, a future is inert until awaited, and dropping it cancels it."
---

| C# | Rust |
| --- | --- |
| async Task<T> F() | async fn f() -> T |
| await t | t.await |
| Task.WhenAll | tokio::join! / join_all |
| Task.WhenAny | tokio::select! |
| CancellationToken | drop the future, or tokio_util CancellationToken |
| Task.Run(blocking) | spawn_blocking |
