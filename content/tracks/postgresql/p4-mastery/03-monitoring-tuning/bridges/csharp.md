---
from: csharp
status: reviewed
concept: "Measuring before tuning"
from_way: "SQL Server's Query Store and DMVs (sys.dm_exec_query_stats), and Application Insights."
native_way: "pg_stat_statements (the closest thing to Query Store's statistics), pg_stat_* views and auto_explain."
sensei_tip: "Coming from Query Store: pg_stat_statements keeps statistics but not plan history. Use auto_explain (or a tool like pganalyze) to keep the plans of slow statements."
---

| SQL Server | PostgreSQL |
| --- | --- |
| Query Store, sys.dm_exec_query_stats | pg_stat_statements |
| captured actual plans | auto_explain |
| sys.dm_exec_requests | pg_stat_activity |
| sys.dm_os_wait_stats | wait_event_type and wait_event |
| Database Engine Tuning Advisor | EXPLAIN plus judgement |
