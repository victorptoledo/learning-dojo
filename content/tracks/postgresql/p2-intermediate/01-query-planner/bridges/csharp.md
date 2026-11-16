---
from: csharp
status: reviewed
concept: "Query execution plans"
from_way: "SQL Server's graphical execution plans in SSMS, or EF Core's ToQueryString() and logging."
native_way: "EXPLAIN (ANALYZE, BUFFERS) gives the same information as text or JSON: nodes, estimates, actual rows, loops and buffers."
sensei_tip: "Coming from SQL Server: PostgreSQL has no query hints and no plan forcing. Fix the statistics or the index; enable_* settings are only for experiments."
---

| C# / SQL Server | PostgreSQL |
| --- | --- |
| Include Actual Execution Plan | EXPLAIN ANALYZE |
| SET STATISTICS IO ON | BUFFERS |
| Key Lookup | Index Scan visiting the heap |
| OPTION (RECOMPILE) | plan_cache_mode = force_custom_plan |
| UPDATE STATISTICS | ANALYZE |
