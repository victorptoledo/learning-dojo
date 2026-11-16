---
from: csharp
status: reviewed
concept: "One logical table, many physical ones"
from_way: "SQL Server partition functions and schemes with SWITCH to move partitions in and out."
native_way: "Declarative partitioning with PARTITION BY and PARTITION OF, and ATTACH and DETACH instead of SWITCH."
sensei_tip: "Coming from SQL Server: there are no global (non-aligned) indexes in PostgreSQL. Every unique key must include the partition key."
---

| C# / SQL Server | PostgreSQL |
| --- | --- |
| CREATE PARTITION FUNCTION ... RANGE RIGHT | PARTITION BY RANGE with FROM (included) TO (excluded) |
| a partition scheme | partitions created with PARTITION OF |
| ALTER TABLE ... SWITCH OUT | DETACH PARTITION |
| SWITCH IN after loading | ATTACH PARTITION, with a matching CHECK to skip validation |
| non-aligned unique indexes | not possible: unique keys include the partition key |
