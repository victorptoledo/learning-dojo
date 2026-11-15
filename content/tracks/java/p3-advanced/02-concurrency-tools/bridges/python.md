---
from: python
status: draft
concept: "Coordinating threads safely"
from_way: "threading.Lock, queue.Queue and concurrent.futures, with the GIL hiding many races."
native_way: "ExecutorService, CompletableFuture, ConcurrentHashMap, atomics and locks, on truly parallel threads."
sensei_tip: "Without a GIL, count += 1 from two Java threads loses updates. Use AtomicLong, ConcurrentHashMap.merge or a lock."
---

| Python | Java 25 |
| --- | --- |
| ThreadPoolExecutor | Executors.newFixedThreadPool |
| future.result() | future.get() or join() |
| threading.Lock | ReentrantLock or synchronized |
| queue.Queue | BlockingQueue |
| dict shared between threads | ConcurrentHashMap |
