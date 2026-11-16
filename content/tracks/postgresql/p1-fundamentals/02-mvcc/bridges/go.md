---
from: go
status: reviewed
concept: "Readers that never block writers"
from_way: "atomic.Pointer or atomic.Value swapping in a new immutable struct while readers keep the old pointer, and the GC freeing it later."
native_way: "MVCC: every update writes a new row version, each transaction reads the versions its snapshot allows, and VACUUM collects the garbage."
sensei_tip: "A tx := db.BeginTx(...) that is never committed because a goroutine returned early stays idle in transaction until the connection closes. Always defer tx.Rollback()."
---

| Go | PostgreSQL |
| --- | --- |
| p.Store(&newConfig) | UPDATE writes a new tuple |
| a goroutine still using the old pointer | a snapshot seeing old versions |
| the GC | VACUUM |
| a forgotten tx | idle in transaction, holding back cleanup |
| copying only the changed part | HOT update on the same page |
