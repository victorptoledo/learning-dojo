---
from: java
status: reviewed
concept: "Query execution plans"
from_way: "Reading Hibernate's SQL log (or a profiler) and guessing why a query is slow in production."
native_way: "EXPLAIN (ANALYZE, BUFFERS) runs the query and shows estimated and actual rows, loops, time and buffers for every node of the plan."
sensei_tip: "Prepared statements from JDBC switch to a generic plan after five executions. If one value of a skewed column is slow only in the app, compare with EXPLAIN (GENERIC_PLAN) or set plan_cache_mode for that role."
---

| Java | PostgreSQL |
| --- | --- |
| spring.jpa.show-sql=true | the query text, without its plan |
| a JFR or async-profiler flame graph | EXPLAIN (ANALYZE, BUFFERS) |
| JIT deoptimisation on new input shapes | a generic plan that fits some parameters badly |
| JMH warm-up | shared buffers: run twice and compare hit and read |
| PreparedStatement | custom plans first, then maybe a generic plan |
