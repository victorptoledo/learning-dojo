---
from: csharp
status: draft
concept: "Coordinating threads safely"
from_way: "Task, lock, Interlocked, ConcurrentDictionary and Parallel."
native_way: "CompletableFuture, synchronized and locks, atomics and ConcurrentHashMap."
sensei_tip: "Interlocked.Increment is AtomicLong.incrementAndGet; ConcurrentDictionary.AddOrUpdate is ConcurrentHashMap.merge; lock is synchronized."
---

| C# | Java 25 |
| --- | --- |
| Task<T> | CompletableFuture<T> |
| lock (o) | synchronized (o) |
| Interlocked.Increment | AtomicLong.incrementAndGet |
| ConcurrentDictionary.AddOrUpdate | ConcurrentHashMap.merge |
| CountdownEvent | CountDownLatch |
