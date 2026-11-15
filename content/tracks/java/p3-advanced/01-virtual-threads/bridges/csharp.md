---
from: csharp
status: draft
concept: "Many concurrent tasks without callbacks"
from_way: "async/await over Task to avoid blocking thread-pool threads."
native_way: "Virtual threads: synchronous code with the scalability of async."
sensei_tip: "Where C# made you write async all the way down, Java 21+ lets you keep synchronous signatures; Executors.newVirtualThreadPerTaskExecutor() is the closest thing to Task.Run for I/O."
---

| C# | Java 25 |
| --- | --- |
| async Task<T> GetAsync() | T get() on a virtual thread |
| Task.WhenAll | invokeAll or StructuredTaskScope (preview) |
| SemaphoreSlim | Semaphore |
| ThreadPool starvation | not an issue for I/O on virtual threads |
