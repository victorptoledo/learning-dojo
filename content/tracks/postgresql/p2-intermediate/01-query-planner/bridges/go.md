---
from: go
status: reviewed
concept: "Query execution plans"
from_way: "pprof profiles and go test -bench to find where time goes."
native_way: "EXPLAIN (ANALYZE, BUFFERS) is the profile of one query: each node with its rows, loops, time and buffers."
sensei_tip: "pgx uses prepared statements by default. A query fast in psql but slow from Go can be a generic plan; check with EXPLAIN (GENERIC_PLAN) or use QueryExecModeExec to compare."
---

| Go | PostgreSQL |
| --- | --- |
| pprof flat and cum | self time and total time per node |
| a function called N times | loops=N |
| allocations | Buffers: shared read |
| benchmarks on realistic data | EXPLAIN ANALYZE on production-sized tables |
| pgx prepared statements | custom then generic plans |
