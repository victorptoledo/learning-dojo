---
from: go
status: reviewed
concept: "Measuring before tuning"
from_way: "pprof, expvar and Prometheus metrics exported from the service."
native_way: "pg_stat_statements for query cost, the pg_stat_* views via postgres_exporter, and alerts on the numbers that predict trouble."
sensei_tip: "pgxpool's Stat() (acquire count, wait duration, idle) next to pg_stat_activity tells you whether the bottleneck is the pool or the database."
---

| Go | PostgreSQL |
| --- | --- |
| pprof CPU profile | pg_stat_statements |
| a flame graph of one request | EXPLAIN (ANALYZE, BUFFERS) |
| expvar / Prometheus counters | postgres_exporter |
| pgxpool.Stat() | pg_stat_activity by state |
| an int32 counter overflowing | a sequence reaching its max_value |
