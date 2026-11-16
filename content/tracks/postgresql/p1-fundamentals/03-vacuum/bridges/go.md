---
from: go
status: reviewed
concept: "Garbage collection for row versions"
from_way: "Go's concurrent GC, tuned with GOGC and GOMEMLIMIT."
native_way: "VACUUM collects dead row versions concurrently, tuned per table with scale factors and cost limits."
sensei_tip: "GOGC=100 means collect after the heap doubles; autovacuum's 0.2 scale factor means vacuum after 20% of the rows died. On a huge table, both wait too long."
---

| Go | PostgreSQL |
| --- | --- |
| concurrent GC | VACUUM / autovacuum |
| GOGC | autovacuum_vacuum_scale_factor |
| GOMEMLIMIT | anti-wraparound vacuum at autovacuum_freeze_max_age |
| runtime.GC() | a manual VACUUM |
| a goroutine leak holding memory | an idle-in-transaction session holding back cleanup |
