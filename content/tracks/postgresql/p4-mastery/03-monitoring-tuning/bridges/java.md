---
from: java
status: reviewed
concept: "Measuring before tuning"
from_way: "Micrometer metrics, JFR recordings and APM traces that show the slowest endpoints, and JVM flags copied from a blog post."
native_way: "pg_stat_statements for the cost of every query shape, the pg_stat_* views for health, and settings changed one at a time from evidence."
sensei_tip: "An APM shows the endpoint; pg_stat_statements shows the query shape and its total cost across all endpoints. Join them through application_name or a comment tag in the SQL."
---

| Java | PostgreSQL |
| --- | --- |
| a JFR hot-methods view | pg_stat_statements by total_exec_time |
| Micrometer gauges | postgres_exporter metrics |
| HikariCP pending-threads metric | pg_stat_activity by state |
| a heap-usage alert | xid age, disk and sequence alerts |
| tuning GC flags after profiling | tuning settings after measuring |
