---
from: go
status: draft
concept: "Concurrency theory in Go"
from_way: "Sharing maps between goroutines without locks."
native_way: "Go offers both models: channels and select for CSP-style message passing, and sync.Mutex, sync.Cond and sync/atomic for shared memory; the Go memory model defines happens-before, and the race detector finds data races at run time."
sensei_tip: "Run tests with -race in CI; use channels to transfer ownership and mutexes to protect state, and never copy a sync.Mutex."
---

| In Go | Underneath |
| --- | --- |
| `ch <- v` / `<-ch` | CSP message passing, a happens-before edge |
| `sync.Mutex` | mutual exclusion |
| `atomic.Int64.CompareAndSwap` | CAS |
| `select` with `time.After` | a timeout |
| "all goroutines are asleep - deadlock!" | the runtime detecting a global deadlock |
