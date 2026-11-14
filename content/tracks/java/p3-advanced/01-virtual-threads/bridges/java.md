---
from: java
status: draft
concept: "Beyond reactive spaghetti"
from_way: "A fixed pool of platform threads per server, or reactive types (Mono/Flux, CompletableFuture chains) to get high concurrency at the cost of readability and stack traces."
native_way: "One virtual thread per task (`Executors.newVirtualThreadPerTaskExecutor()`), plain blocking code, and the JVM unmounts the thread while it waits."
sensei_tip: "With virtual threads, plain synchronous code is a strong default again for I/O-heavy services; keep reactive code for streaming and backpressure."
---

| Before | With virtual threads |
| --- | --- |
| `Executors.newFixedThreadPool(200)` for I/O tasks | `Executors.newVirtualThreadPerTaskExecutor()` |
| `supplyAsync(...).thenCompose(...).thenCombine(...)` | Two `submit` calls and two `get()` |
| Tuning `server.tomcat.threads.max` | `spring.threads.virtual.enabled=true` |
| `executor.shutdown(); executor.awaitTermination(...)` | `try (var executor = ...) { ... }` |
| Limiting load with the thread pool size | A `Semaphore` or the connection pool |
