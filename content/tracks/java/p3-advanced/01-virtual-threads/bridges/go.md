---
from: go
status: draft
concept: "Many concurrent tasks without callbacks"
from_way: "Goroutines: cheap, blocking, scheduled by the Go runtime."
native_way: "Virtual threads, the same idea in the JVM."
sensei_tip: "Virtual threads are Java's goroutines: cheap, parked on I/O, scheduled onto a few carrier threads. Java's structured concurrency API plays the role of errgroup."
---

| Go | Java 25 |
| --- | --- |
| go handle(c) | Thread.ofVirtual().start(() -> handle(c)) |
| errgroup.Group | StructuredTaskScope (preview) or an executor |
| semaphore channel | Semaphore |
| GOMAXPROCS | carrier threads in the ForkJoinPool |
