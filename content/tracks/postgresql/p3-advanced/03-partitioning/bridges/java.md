---
from: java
status: reviewed
concept: "One logical table, many physical ones"
from_way: "Rolling log files by day with Logback, or sharding data by hand into tables named events_2026_11 and choosing the table in Java code."
native_way: "Declarative partitioning: one table name for the application, partitions chosen by the database on insert and skipped by the planner on read."
sensei_tip: "Hibernate needs no change to use a partitioned table, but its @Id must match the composite primary key (id plus the partition key) or use a natural id; plan the entity mapping before you partition."
---

| Java | PostgreSQL |
| --- | --- |
| a log file per day, rolled by Logback | a range partition per day or month |
| deleting old log files | DETACH PARTITION and DROP TABLE |
| choosing the table in a DAO | routing by the partition key, done by the database |
| a ConcurrentHashMap's segments | hash partitions |
| a scheduled job that creates next month's file | a job (or pg_partman) creating future partitions |
