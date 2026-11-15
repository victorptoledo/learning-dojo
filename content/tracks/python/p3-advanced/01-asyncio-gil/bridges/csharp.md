---
from: csharp
status: draft
concept: "async/await and the thread pool"
from_way: "Tasks on the thread pool that run in parallel, async and await, Task.WhenAll, CancellationToken passed everywhere, SemaphoreSlim for limits, and Parallel.For for CPU work."
native_way: "Coroutines on a single-threaded event loop, async and await, TaskGroup and gather, cancellation by CancelledError without tokens, asyncio.Semaphore, and processes for CPU work."
sensei_tip: "No CancellationToken parameters: asyncio cancels a task by raising CancelledError at its current await. Your job is to not swallow it and to clean up in finally."
---

| C# | Python |
| --- | --- |
| `async Task<int> FetchAsync()` | `async def fetch() -> int:` |
| `await Task.WhenAll(tasks)` | `TaskGroup` / `await asyncio.gather(*coros)` |
| `CancellationToken` | `task.cancel()` and `CancelledError` |
| `cts.CancelAfter(3000)` | `async with asyncio.timeout(3):` |
| `SemaphoreSlim(50)` + `WaitAsync` | `asyncio.Semaphore(50)` + `async with` |
| `Task.Run(() => Blocking())` | `await asyncio.to_thread(blocking)` |
| `Parallel.For` (CPU work) | `ProcessPoolExecutor` |
| `Channel<T>` | `asyncio.Queue` |
