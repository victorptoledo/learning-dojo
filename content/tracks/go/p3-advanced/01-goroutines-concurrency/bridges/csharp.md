---
from: csharp
status: draft
concept: "Tasks, async and await"
from_way: "Task.Run and the thread pool, async methods that return Task and spread async through the call chain, Task.WhenAll for fan-out, and CancellationToken for cancellation."
native_way: "go starts a goroutine, functions stay synchronous, a WaitGroup or channel replaces Task.WhenAll, and context.Context plays the role of CancellationToken."
sensei_tip: "async in C# spreads through every signature. In Go, write the synchronous function first; concurrency is added at the call site with go."
---

| C# | Go |
| --- | --- |
| `Task.Run(() => Work())` | `go work()` |
| `await Task.WhenAll(tasks)` | `wg.Wait()` |
| `async Task<string> FetchAsync()` | `func Fetch(ctx context.Context) (string, error)` |
| thread pool | scheduler with GOMAXPROCS Ps |
| `CancellationToken` | `context.Context` |
| `Parallel.ForEach(..., maxDegree)` | N worker goroutines or a semaphore channel |
| `AggregateException` from WhenAll | `errgroup` or `errors.Join` |
