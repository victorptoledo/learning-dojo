---
from: javascript
status: draft
concept: "Agreeing on one owner"
from_way: "One Node process where the event loop serialises everything, or a cron job on one host."
native_way: "Leases from etcd or a database with renewals, and fencing tokens attached to every write the leader makes."
sensei_tip: "A long synchronous computation blocks the event loop the way a GC pause blocks a JVM: lease renewals stop. Keep the leader's work chunked, and fence every write."
---

| JavaScript | Consensus concept |
| --- | --- |
| a single event loop | mutual exclusion inside one process |
| `etcd3` `lease(ttl)` and `election` | leader election on etcd |
| `redlock` | an efficiency lock, not a correctness one |
| conditional `UPDATE` with a token | a fenced write |
| `performance.now()` | a monotonic clock |
