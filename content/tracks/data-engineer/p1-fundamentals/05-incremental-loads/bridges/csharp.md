---
from: csharp
status: draft
concept: "Safe, repeatable loads"
from_way: "SqlBulkCopy into the target after querying rows modified since the last run."
native_way: "Bulk copy into a staging table, then a MERGE into the target, then advance the watermark."
sensei_tip: "SQL Server's MERGE needs care (use HOLDLOCK and test it); many teams prefer UPDATE then INSERT WHERE NOT EXISTS in one transaction. Either way, bulk copy goes to staging, never straight to the target."
---

| C# | Incremental loads |
| --- | --- |
| SqlBulkCopy into staging | a fast landing step |
| MERGE ... WITH (HOLDLOCK) | an idempotent upsert |
| rowversion column | a reliable change marker |
| Change Tracking in SQL Server | changes including deletes |
| TransactionScope | watermark saved with the load |
