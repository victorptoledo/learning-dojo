---
from: java
status: reviewed
concept: "Garbage collection for row versions"
from_way: "The JVM's garbage collector: G1 or ZGC run concurrently, tuned with heap and pause settings, and a full GC stops the world."
native_way: "VACUUM collects dead row versions concurrently; VACUUM FULL is the stop-the-world compaction; autovacuum is the collector's scheduler."
sensei_tip: "Treat VACUUM FULL like System.gc() in a hot loop: almost never the answer. Make autovacuum run earlier and faster on the big tables instead."
---

| Java | PostgreSQL |
| --- | --- |
| concurrent GC (G1, ZGC) | VACUUM and autovacuum |
| full GC, stop the world | VACUUM FULL (ACCESS EXCLUSIVE lock) |
| GC tuning flags | autovacuum_vacuum_scale_factor, cost_limit, per table |
| a static map that leaks | a long transaction holding back the xmin horizon |
| -Xlog:gc | log_autovacuum_min_duration |
