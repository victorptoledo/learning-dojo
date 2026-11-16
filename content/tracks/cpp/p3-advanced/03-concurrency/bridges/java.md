---
from: java
status: draft
concept: "Threads and the memory model"
from_way: "Thread, ExecutorService, synchronized and ReentrantLock, wait/notify, volatile, AtomicInteger and CompletableFuture; the JMM defines happens-before."
native_way: "std::jthread, std::mutex with RAII locks, condition variables, std::atomic with memory orders, std::future; a data race is undefined behaviour."
sensei_tip: "A Java data race gives odd values; a C++ data race is undefined behaviour. And `volatile` in C++ is not for threads: use std::atomic."
---

| Java | C++ |
| --- | --- |
| `new Thread(r).start()` | `std::jthread t(r);` (joins in its destructor) |
| `synchronized (lock) { }` | `std::lock_guard g(mutex);` |
| `wait()` / `notify()` | `std::condition_variable` + predicate |
| `volatile boolean` | `std::atomic<bool>` |
| `AtomicInteger.incrementAndGet()` | `++counter` on `std::atomic<int>` |
| `ExecutorService` + `Future` | a pool + `std::packaged_task` / `std::future` |
| `CountDownLatch` | `std::latch` |
