---
from: csharp
status: draft
concept: "Threads and concurrency"
from_way: "Thread, Task and async/await, lock, Monitor, SemaphoreSlim, Interlocked, volatile and BlockingCollection."
native_way: "std::jthread, std::mutex and RAII locks, condition variables, std::atomic, std::counting_semaphore and std::future."
sensei_tip: "BlockingCollection<T> with CompleteAdding() is exactly the blocking queue with close() you write in the kata."
---

| C# | C++ |
| --- | --- |
| `lock (obj) { }` | `std::lock_guard g(m);` |
| `Interlocked.Increment(ref n)` | `++n` on `std::atomic<int>` |
| `Monitor.Wait` / `Pulse` | `std::condition_variable` |
| `BlockingCollection<T>` | a blocking queue |
| `Task.Run(f)` + `await` | `std::async` / a pool + `std::future::get()` |
| `CancellationToken` | `std::stop_token` |
| `SemaphoreSlim` | `std::counting_semaphore` |
