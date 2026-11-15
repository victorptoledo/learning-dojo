---
from: python
status: draft
concept: "Locks, the GIL and shared state"
from_way: "threading.Lock and RLock, the GIL making single bytecode operations look atomic, dict operations that rarely corrupt, functools.cache for lazy values, and no tool that detects races."
native_way: "Goroutines run truly in parallel, so every shared write needs a Mutex, an atomic or a channel; maps can crash the program under concurrent writes; and -race detects races."
sensei_tip: "Habits that were safe under the GIL, like counter += 1 from several threads on a dict, are real bugs in Go. Assume nothing is atomic unless sync/atomic says so."
---

| Python | Go |
| --- | --- |
| `lock = threading.Lock()` / `with lock:` | `var mu sync.Mutex` / `mu.Lock(); defer mu.Unlock()` |
| `threading.RLock()` | no reentrant mutex: restructure the code |
| GIL-protected `count += 1` | `atomic.Int64.Add(1)` |
| shared `dict` | `map` + mutex (concurrent writes crash) |
| `functools.cache` on a loader | `sync.OnceValue` |
| `threading.Event` | `close(done)` or `atomic.Bool` |
| no race detection | `go test -race` |
