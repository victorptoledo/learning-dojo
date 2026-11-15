---
from: go
status: draft
concept: "Coordinating threads safely"
from_way: "Goroutines, channels, sync.Mutex, sync.WaitGroup and atomic."
native_way: "Executors, futures, locks, latches and concurrent collections."
sensei_tip: "sync.WaitGroup is a CountDownLatch or simply joining futures; a buffered channel is a BlockingQueue; sync.Map is a ConcurrentHashMap."
---

| Go | Java 25 |
| --- | --- |
| sync.WaitGroup | CountDownLatch / invokeAll |
| chan T | BlockingQueue<T> |
| sync.Mutex | ReentrantLock |
| atomic.Int64 | AtomicLong |
| sync.Map | ConcurrentHashMap |
