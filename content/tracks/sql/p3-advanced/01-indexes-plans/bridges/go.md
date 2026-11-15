---
from: go
status: draft
concept: "Access paths and the plan that uses them"
from_way: "A map for lookups, sort.Search for ranges, and pprof for profiling."
native_way: "Indexes for lookups and ranges, and EXPLAIN ANALYZE as the query profiler."
sensei_tip: "sort.Search on a sorted slice is an index range scan; a map is an index lookup. Keep the migrations that create indexes next to the queries that need them."
---

| Go | SQL |
| --- | --- |
| m[id] | WHERE id = $1 with a primary key |
| sort.Search(len(s), func(i int) bool { return s[i].At >= from }) | a range scan on an index |
| slices sorted by (CustomerID, At) | a composite index |
| pprof | EXPLAIN (ANALYZE, BUFFERS) |
