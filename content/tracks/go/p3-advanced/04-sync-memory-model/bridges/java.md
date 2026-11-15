---
from: java
status: draft
concept: "synchronized, java.util.concurrent and the JMM"
from_way: "synchronized blocks and ReentrantLock, volatile for visibility, AtomicLong, ConcurrentHashMap as the default shared map, and the Java Memory Model's happens-before rules."
native_way: "sync.Mutex (not reentrant), atomic types instead of volatile, a map plus a mutex as the default, sync.Map for narrow cases, and a memory model with the same happens-before idea and a race detector to check it."
sensei_tip: "Go has no volatile. If a flag is read by one goroutine and written by another, it must be an atomic.Bool or be guarded by a mutex, full stop."
---

| Java | Go |
| --- | --- |
| `synchronized (this) { ... }` | `c.mu.Lock(); defer c.mu.Unlock()` |
| `ReentrantLock` | `sync.Mutex` (not reentrant) |
| `ReentrantReadWriteLock` | `sync.RWMutex` |
| `volatile boolean ready` | `var ready atomic.Bool` |
| `AtomicLong` | `atomic.Int64` |
| `ConcurrentHashMap` | `map` + `sync.Mutex`, or `sync.Map` |
| lazy holder idiom / double-checked locking | `sync.OnceValue` |
| `ThreadLocal` buffers | `sync.Pool` |
| no built-in race checker | `go test -race` |
