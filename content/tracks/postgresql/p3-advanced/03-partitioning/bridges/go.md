---
from: go
status: reviewed
concept: "One logical table, many physical ones"
from_way: "Building table names such as events_202611 in Go code and writing to the right one."
native_way: "Declarative partitioning: the application writes to events, and the database routes and prunes."
sensei_tip: "Prepared statements with a time parameter still prune, at execution time; look for Subplans Removed in EXPLAIN ANALYZE of the generic plan."
---

| Go | PostgreSQL |
| --- | --- |
| fmt.Sprintf("events_%s", month) | the partition chosen by the database |
| a goroutine that rotates tables | a job or pg_partman creating partitions |
| os.Remove on old files | DETACH PARTITION and DROP TABLE |
| hash/fnv to pick a bucket | HASH partitioning |
