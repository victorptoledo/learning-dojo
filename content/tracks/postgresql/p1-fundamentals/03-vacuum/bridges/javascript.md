---
from: javascript
status: reviewed
concept: "Garbage collection for row versions"
from_way: "V8's incremental and concurrent garbage collector, which you almost never tune, and heap snapshots to find leaks."
native_way: "VACUUM collects dead row versions; on big tables you do tune autovacuum, per table."
sensei_tip: "A Node service whose pool keeps a client checked out inside BEGIN after an exception leaves a session idle in transaction. Always release in finally, after COMMIT or ROLLBACK."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| V8 GC | VACUUM / autovacuum |
| a full mark-compact pause | VACUUM FULL |
| --max-old-space-size and friends | autovacuum settings per table |
| a leak through a closure | an idle-in-transaction client |
| heap snapshot | pgstattuple, n_dead_tup |
