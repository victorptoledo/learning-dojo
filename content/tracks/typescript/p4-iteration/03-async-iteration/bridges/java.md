---
from: java
status: draft
concept: "Blocking iteration on virtual threads, and reactive streams"
from_way: "A blocking Iterator or BufferedReader.lines() on a virtual thread, or Flow.Publisher and Reactor's Flux with request(n) for backpressure; try-with-resources closes the source."
native_way: "AsyncIterable<T> consumed with for await on the event loop; pulling is the backpressure, return() closes the source, AbortSignal cancels."
sensei_tip: "for await reads like blocking Java code on a virtual thread, and it behaves like a Flux with request(1): the producer only runs when the consumer asks."
---

| Java | TypeScript |
| --- | --- |
| `Files.lines(path)` in try-with-resources | `readline.createInterface({ input: createReadStream(path) })` in `for await` |
| `Flux<T>` / `Flow.Publisher<T>` | `AsyncIterable<T>` |
| `subscription.request(n)` | Each `await iterator.next()` pulls one |
| `flux.flatMapSequential(f, 4)` | A `mapConcurrent(source, f, { concurrency: 4 })` helper |
| `subscription.cancel()` | `break` (calls `return()`) or `controller.abort()` |
| `try (var r = ...)` | `finally` in the async generator, or `await using` |
