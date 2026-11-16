---
from: python
status: reviewed
concept: "Query execution plans"
from_way: "cProfile output and Django Debug Toolbar's SQL panel showing which queries are slow."
native_way: "EXPLAIN (ANALYZE, BUFFERS) shows why a query is slow: estimated versus actual rows, loops and buffers per node."
sensei_tip: "Django's queryset.explain(analyze=True, buffers=True) passes the options straight to PostgreSQL. Run it on production-sized data; a plan on a 100-row dev table tells you nothing."
---

| Python | PostgreSQL |
| --- | --- |
| cProfile ncalls | loops |
| cumulative time | actual time (per loop) × loops |
| qs.explain(analyze=True) | EXPLAIN ANALYZE |
| pandas df.describe() | pg_stats for a column |
| a cache-hit counter | Buffers: shared hit |
