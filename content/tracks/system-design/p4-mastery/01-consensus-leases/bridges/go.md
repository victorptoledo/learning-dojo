---
from: go
status: draft
concept: "Agreeing on one owner"
from_way: "sync.Mutex in one process."
native_way: "etcd's concurrency package: sessions with leases, elections and mutexes, plus a revision you can use as a fencing token."
sensei_tip: "In etcd, a key's create revision only grows; concurrency.Election and Mutex expose it, so the leader can attach it to writes as a fencing token. etcd and its Raft library are written in Go."
---

| Go | Consensus concept |
| --- | --- |
| `sync.Mutex` | mutual exclusion in one process |
| `concurrency.NewSession(cli, concurrency.WithTTL(10))` | a lease kept alive |
| `concurrency.NewElection(s, "/leader").Campaign(...)` | leader election |
| `Mutex.Header().Revision` | a fencing token |
| `time.Since(start)` (monotonic) | lease durations |
