---
from: java
status: draft
concept: "Blocking queues and futures"
from_way: "BlockingQueue between producer and consumer threads, poison pills to signal the end, CompletableFuture.anyOf for the first result, Semaphore for limits and ScheduledExecutorService for timeouts."
native_way: "Channels are typed blocking queues built into the language, close replaces the poison pill, select waits on several channels at once, and a buffered channel is a semaphore."
sensei_tip: "select has no Java equivalent: it is what you wish poll(timeout) on several queues at once looked like."
---

| Java | Go |
| --- | --- |
| `new SynchronousQueue<T>()` | `make(chan T)` |
| `new ArrayBlockingQueue<T>(10)` | `make(chan T, 10)` |
| `queue.put(x)` / `queue.take()` | `ch <- x` / `x := <-ch` |
| poison pill object | `close(ch)` and `for v := range ch` |
| `queue.poll(2, SECONDS)` | `select { case v := <-ch: ... case <-time.After(2 * time.Second): ... }` |
| `CompletableFuture.anyOf(...)` | `select` over result channels |
| `new Semaphore(3)` | `make(chan struct{}, 3)` |
| `ExecutorService` with N threads | N goroutines ranging over a jobs channel |
