---
from: java
status: draft
concept: "Concurrency theory in Java"
from_way: "Using volatile as a lock, or synchronized everything."
native_way: "The Java Memory Model defines happens-before (locks, volatile, thread start and join, final fields); java.util.concurrent provides atomics (CAS), locks, condition variables, semaphores and lock-free collections."
sensei_tip: "Read the happens-before rules of the JMM once; then prefer high-level tools (ConcurrentHashMap, executors, CompletableFuture) and test hard cases with jcstress."
---

| In Java | Underneath |
| --- | --- |
| `synchronized` | a monitor: lock + wait/notify |
| `volatile` | visibility and ordering, not atomic ++ |
| `AtomicLong.compareAndSet` | CAS |
| `ReentrantLock.tryLock(timeout)` | breaks hold-and-wait |
| `ConcurrentLinkedQueue` | the lock-free Michael-Scott queue |
