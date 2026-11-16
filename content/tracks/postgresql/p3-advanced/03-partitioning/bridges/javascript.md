---
from: javascript
status: reviewed
concept: "One logical table, many physical ones"
from_way: "Writing to a collection per month in MongoDB, or a TTL index that deletes old documents one by one."
native_way: "Declarative partitioning: one table for the application, partitions dropped whole when they expire."
sensei_tip: "A TTL index deletes row by row, like a nightly DELETE. Dropping a whole partition is instant and leaves nothing for VACUUM."
---

| JavaScript / TypeScript | PostgreSQL |
| --- | --- |
| a collection per month | a partition per month |
| a TTL index | DETACH PARTITION and DROP TABLE |
| choosing a collection in code | routing by the partition key, done by the database |
| sharding by hash of a key | HASH partitions |
