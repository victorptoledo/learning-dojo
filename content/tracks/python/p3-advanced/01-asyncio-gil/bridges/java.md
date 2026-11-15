---
from: java
status: draft
concept: "Threads, virtual threads and an event loop"
from_way: "OS threads that run Java code truly in parallel, ExecutorService pools, virtual threads that make blocking calls cheap, CompletableFuture, and StructuredTaskScope."
native_way: "Threads limited by the GIL for CPU work, processes for parallelism, and asyncio's single-threaded event loop where every wait is an explicit await; TaskGroup is structured concurrency."
sensei_tip: "Virtual threads hide the waiting; asyncio makes it visible with await, and one forgotten blocking call stalls everything. For CPU work, Java threads scale and standard Python threads do not: use processes."
---

| Java | Python |
| --- | --- |
| `Executors.newFixedThreadPool(20)` | `ThreadPoolExecutor(max_workers=20)` |
| parallel CPU work on threads | `ProcessPoolExecutor` (or the free-threaded build) |
| virtual threads for blocking I/O | `asyncio` coroutines with `await` |
| `CompletableFuture.allOf(...)` | `asyncio.gather(...)` |
| `StructuredTaskScope` | `asyncio.TaskGroup` |
| `future.get(3, SECONDS)` | `async with asyncio.timeout(3):` |
| `Semaphore(50)` | `asyncio.Semaphore(50)` |
| `synchronized` / `ReentrantLock` | `threading.Lock` (`asyncio.Lock` inside coroutines) |
