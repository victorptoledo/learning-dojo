---
from: javascript
status: reviewed
concept: "Measuring before tuning"
from_way: "Lighthouse scores, APM traces and console timing in the browser and Node."
native_way: "pg_stat_statements, the pg_stat_* views and a few alerts that predict trouble."
sensei_tip: "Tag queries with the route that sent them (for example a SQL comment added by a sqlcommenter library) so pg_stat_statements and the slow log point back to your code."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| a performance profile | EXPLAIN (ANALYZE, BUFFERS) |
| the slowest routes in an APM | pg_stat_statements by mean_exec_time |
| the busiest routes | pg_stat_statements by total_exec_time |
| an uptime check | pg_isready and connection metrics |
| an error-rate alert | deadlocks, conflicts, archiver failures |
