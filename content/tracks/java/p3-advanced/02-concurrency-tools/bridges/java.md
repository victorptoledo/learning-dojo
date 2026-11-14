---
from: java
status: draft
concept: "Concurrency after Java 8"
from_way: "shutdown() and awaitTermination() by hand, CompletableFuture chains on the common pool, synchronized everywhere, and ThreadLocal for request context."
native_way: "AutoCloseable executors, Future.state() and resultNow(), CompletableFuture with your own executor and orTimeout, locks only where invariants need them, and ScopedValue."
sensei_tip: "Before adding a lock, ask whether the task could return its result instead of writing it into shared state."
---

| Java 8 | Java 25 |
| --- | --- |
| `executor.shutdown(); executor.awaitTermination(1, MINUTES);` | `try (var executor = ...) { ... }` |
| `future.get(1, SECONDS)` and a custom timeout map | `cf.orTimeout(1, SECONDS)` / `completeOnTimeout(...)` |
| `ThreadLocal<User>` set and removed by a filter | `ScopedValue.where(USER, user).run(...)` |
| `Collections.synchronizedMap(new HashMap<>())` | `new ConcurrentHashMap<>()` |
| `AtomicLong` for hot counters | `LongAdder` |
