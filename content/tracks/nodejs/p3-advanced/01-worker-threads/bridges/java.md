---
from: java
status: draft
concept: "Threads that share nothing"
from_way: "Threads share the heap; ExecutorService pools run tasks; synchronized, locks and concurrent collections protect shared state."
native_way: "Worker threads have separate heaps and talk with messages; a pool (Piscina or your own) runs tasks; only SharedArrayBuffer plus Atomics is shared."
sensei_tip: "Think of each worker as an actor, not a Java thread: no shared objects, so no locks, but every message is copied unless you transfer it."
---

| Java | Node.js |
| --- | --- |
| `Executors.newFixedThreadPool(n)` | A pool of `n` workers (Piscina) |
| `executor.submit(task).get()` | `await pool.run(task)` |
| Shared heap + `synchronized` | Messages; `SharedArrayBuffer` + `Atomics` |
| `Runtime.availableProcessors()` | `os.availableParallelism()` |
| `Thread.UncaughtExceptionHandler` | `worker.on("error")` |
