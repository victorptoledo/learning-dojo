---
from: csharp
status: draft
concept: "lock, Interlocked and concurrent collections"
from_way: "lock statements on a private object, Interlocked.Increment, volatile fields, ConcurrentDictionary, Lazy<T> for one-time initialisation and ReaderWriterLockSlim."
native_way: "sync.Mutex next to the data, atomic types, a map plus a mutex or sync.Map, sync.OnceValue for lazy values and sync.RWMutex, all checked by the race detector."
sensei_tip: "Lazy<T> with thread safety is sync.OnceValue in one line: var load = sync.OnceValue(func() T { ... })."
---

| C# | Go |
| --- | --- |
| `lock (_gate) { ... }` | `mu.Lock(); defer mu.Unlock()` |
| `Interlocked.Increment(ref n)` | `n.Add(1)` on an `atomic.Int64` |
| `volatile bool _ready` | `atomic.Bool` |
| `ConcurrentDictionary<K, V>` | `map` + `sync.Mutex`, or `sync.Map` |
| `Lazy<T>` | `sync.OnceValue` |
| `ReaderWriterLockSlim` | `sync.RWMutex` |
| `ArrayPool<byte>.Shared` | `sync.Pool` |
| Concurrency Visualizer | `go test -race`, the execution tracer |
