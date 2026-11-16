---
from: csharp
status: draft
concept: "Continuations on one thread"
from_way: "await resumes on a thread-pool thread (no SynchronizationContext in ASP.NET Core), so continuations of different requests run in parallel."
native_way: "await resumes as a microtask on the one main thread, so continuations never run in parallel, and a long synchronous section delays all of them."
sensei_tip: "Node.js behaves like a UI thread with a SynchronizationContext: everything after await comes back to the same thread. That removes data races, and it makes blocking calls far more expensive."
---

| C# / .NET | Node.js |
| --- | --- |
| Thread pool continuations | Microtask queue on the main thread |
| `Task.Yield()` | `await setImmediate()` |
| `Task.Run(cpuWork)` | Worker thread |
| `ThreadPool.SetMinThreads` | `UV_THREADPOOL_SIZE` |
| `dotnet-counters` thread pool queue | Event loop delay |
