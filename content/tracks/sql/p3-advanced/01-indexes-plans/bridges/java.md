---
from: java
status: draft
concept: "Access paths and the plan that uses them"
from_way: "A HashMap or TreeMap built for fast lookups, and a profiler to see where time goes."
native_way: "B-tree indexes (a TreeMap on disk) and EXPLAIN ANALYZE as the query's profiler."
sensei_tip: "Turn on Hibernate's SQL logging, copy the slow statement, and run EXPLAIN (ANALYZE, BUFFERS) on it with real parameters; the plan tells you which index is missing."
---

| Java | SQL |
| --- | --- |
| TreeMap<LocalDateTime, Order> | CREATE INDEX ON orders (placed_at) |
| map.subMap(from, to) | WHERE placed_at >= from AND placed_at < to |
| a Comparator on (customerId, placedAt) | a composite index (customer_id, placed_at) |
| @Index(columnList = "customer_id, placed_at") | the same index declared in JPA |
| a profiler flame graph | EXPLAIN ANALYZE |
