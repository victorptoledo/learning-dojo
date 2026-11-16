---
from: java
status: draft
concept: "Non-blocking I/O on a single thread"
from_way: "One thread per request, each blocked until the database answers; a busy thread only delays its own request."
native_way: "A single thread runs JavaScript while asynchronous I/O (disk, network) is delegated to libuv and its thread pool; callbacks run one at a time in the loop's phases."
sensei_tip: "Never block the main thread: heavy CPU work (hashing, parsing a huge JSON) freezes every user of the server. It is like doing CPU work on Netty's event loop thread instead of an executor."
---

| Java | Node.js |
| --- | --- |
| Tomcat worker threads | The event loop thread |
| `ExecutorService` for blocking work | libuv thread pool, worker threads |
| Netty `EventLoop.execute(task)` | `setImmediate(task)` |
| `CompletableFuture.thenApply` | Promise `.then` (microtask queue) |
| Thread dumps to find a stuck thread | `--cpu-prof`, event loop delay |
