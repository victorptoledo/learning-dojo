---
from: python
status: draft
concept: "Agreeing on one owner"
from_way: "threading.Lock in one process, or a cron job on a single server."
native_way: "A lease in etcd, ZooKeeper or a database, renewed in the background, plus a fencing token checked by the resource."
sensei_tip: "time.monotonic() is the clock for lease durations; time.time() can jump. kazoo's Election recipe runs a function only on the elected leader of a ZooKeeper group."
---

| Python | Consensus concept |
| --- | --- |
| `threading.Lock` | mutual exclusion in one process |
| kazoo `Election` / `Lock` | ZooKeeper leader election and locks |
| `etcd3` lease with `refresh()` | an etcd lease |
| `UPDATE ... WHERE token < %s` | a fenced write in SQL |
| `time.monotonic()` | the monotonic clock |
