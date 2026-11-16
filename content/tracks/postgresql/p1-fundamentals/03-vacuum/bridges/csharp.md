---
from: csharp
status: reviewed
concept: "Garbage collection for row versions"
from_way: "The .NET GC with generations and background collection, and GC.Collect() as a last resort."
native_way: "VACUUM collects dead row versions concurrently, autovacuum schedules it, VACUUM FULL compacts with an exclusive lock."
sensei_tip: "Like GC.Collect(), VACUUM FULL is a smell in production; fix the long transactions and the autovacuum settings that let bloat grow."
---

| C# | PostgreSQL |
| --- | --- |
| background GC | autovacuum |
| GC.Collect(2, Compacting) | VACUUM FULL |
| GCSettings, server GC | autovacuum_vacuum_cost_limit, scale factors |
| a static event handler that leaks | a long transaction or forgotten replication slot |
| dotnet-counters | pg_stat_user_tables, pg_stat_progress_vacuum |
