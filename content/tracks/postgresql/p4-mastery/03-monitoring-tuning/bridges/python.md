---
from: python
status: reviewed
concept: "Measuring before tuning"
from_way: "py-spy, Sentry performance traces and Django Debug Toolbar's query counts."
native_way: "pg_stat_statements for the cost of every query shape, pg_stat_* views for health, and evidence-based settings."
sensei_tip: "An N+1 pattern from the ORM shows up in pg_stat_statements as one tiny query with millions of calls at the top of total_exec_time. Fix it with select_related or prefetch_related."
---

| Python | PostgreSQL |
| --- | --- |
| py-spy top | pg_stat_statements |
| Sentry slow transaction | auto_explain in the log |
| Prometheus client metrics | postgres_exporter |
| a queue-depth alert | connections by state, lock waits |
| changing one setting and re-running the benchmark | one change at a time, measured |
