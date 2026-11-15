---
from: java
status: draft
concept: "Agreeing on one owner"
from_way: "synchronized blocks and ReentrantLock, which give mutual exclusion inside one JVM."
native_way: "Leases from a consensus-backed store across machines, with fencing tokens so a paused holder cannot do damage."
sensei_tip: "A ReentrantLock cannot be lost while you hold it; a distributed lease can, during a GC pause. Apache Curator's LeaderLatch and InterProcessMutex wrap ZooKeeper; pass the session or a version as a fencing token to whatever you write."
---

| Java | Consensus concept |
| --- | --- |
| `synchronized` / `ReentrantLock` | mutual exclusion in one process |
| Curator `LeaderLatch` | leader election on ZooKeeper |
| jetcd `Lease.grant(ttl)` + `keepAlive` | an etcd lease and renewal |
| `@Version` (JPA optimistic locking) | a conditional write, fencing-like |
| `System.nanoTime()` | the monotonic clock |
| ShedLock | scheduled-job locks (efficiency, add idempotency) |
