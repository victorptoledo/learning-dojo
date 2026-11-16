---
from: csharp
status: draft
concept: "Concurrency theory in C#"
from_way: "Locking on this or on strings, and mixing blocking with async."
native_way: "lock is a monitor; Interlocked provides CAS and atomic adds; volatile and Volatile.Read give ordering; SemaphoreSlim works with async; System.Threading.Channels give message passing."
sensei_tip: "Use SemaphoreSlim.WaitAsync instead of lock around await, Interlocked for counters, and Channels for producer-consumer pipelines."
---

| In C# | Underneath |
| --- | --- |
| `lock (obj)` | Monitor.Enter / Exit |
| `Interlocked.CompareExchange` | CAS |
| `SemaphoreSlim.WaitAsync` | an async semaphore |
| `Channel<T>` | a bounded buffer with back-pressure |
| `.Result` on a Task | a classic deadlock with a sync context |
