---
from: java
status: reviewed
concept: "Readers that never block writers"
from_way: "Immutable objects and copy-on-write collections such as CopyOnWriteArrayList, where readers keep the old array while a writer builds a new one."
native_way: "MVCC: every update writes a new row version, each transaction reads the versions its snapshot allows, and VACUUM collects the garbage."
sensei_tip: "MVCC is copy-on-write with a garbage collector. Like a long GC pause, a long-open transaction (a @Transactional method calling a slow service) keeps every old version alive."
---

| Java | PostgreSQL |
| --- | --- |
| CopyOnWriteArrayList.set(...) | UPDATE writes a new tuple |
| a reader holding the old array | a snapshot seeing old versions |
| the garbage collector | VACUUM |
| a reference that keeps objects alive | a long transaction's snapshot (backend_xmin) |
| @Version field | xmin, usable as an optimistic lock column |
