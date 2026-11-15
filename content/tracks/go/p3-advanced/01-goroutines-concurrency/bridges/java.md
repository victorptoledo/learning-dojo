---
from: java
status: draft
concept: "Threads, executors and virtual threads"
from_way: "Platform threads that are expensive, ExecutorService pools sized by hand, CompletableFuture chains, and since Java 21 virtual threads mounted on carrier threads."
native_way: "Goroutines have always been what virtual threads are in Java 21: cheap, scheduled by the runtime onto a few OS threads, with blocking calls parked by the runtime."
sensei_tip: "If you know virtual threads, you know goroutines. The differences: no handle to join or interrupt, and cancellation goes through context instead of Thread.interrupt."
---

| Java | Go |
| --- | --- |
| `Thread.ofVirtual().start(task)` | `go task()` |
| `executor.submit(task)` + `Future.get()` | `go` + a channel or `wg.Wait()` |
| `CountDownLatch` | `sync.WaitGroup` |
| `ExecutorService` with N threads | N worker goroutines reading a channel |
| `thread.interrupt()` | `cancel()` of a `context.Context` |
| `thread.join()` | `wg.Wait()` or receiving from a done channel |
| carrier threads | Ps and Ms (GOMAXPROCS) |
| `-XX:ActiveProcessorCount` in containers | automatic since Go 1.25 |
| thread dump (`jstack`) | goroutine profile, `SIGQUIT` stack dump |
