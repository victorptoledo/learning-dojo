---
from: go
status: draft
concept: "What a read is allowed to return"
from_way: "The Go memory model: channels and mutexes order goroutines' reads and writes."
native_way: "Ordering and visibility across machines, provided by consensus (etcd) or quorums, at a price in latency."
sensei_tip: "etcd's client offers linearizable reads by default and serializable (possibly stale) reads with clientv3.WithSerializable(): the CAP and PACELC trade-off as a single option."
---

| Go | Distributed consistency |
| --- | --- |
| `sync.Mutex` / channels | ordering in one process |
| etcd `Get(ctx, k)` | linearizable read (default) |
| etcd `Get(ctx, k, clientv3.WithSerializable())` | local, possibly stale read |
| etcd `Txn().If(...).Then(...)` | a conditional write |
| gocql `gocql.Quorum` | quorum reads and writes |
