---
from: csharp
status: reviewed
concept: "Choosing an index structure"
from_way: "SQL Server's clustered and nonclustered indexes, filtered indexes and INCLUDE columns, or HasIndex in EF Core."
native_way: "Six index types for six shapes of data and query, plus partial (filtered) and expression indexes."
sensei_tip: "Npgsql's EF Core provider has HasMethod(\"gin\"), HasFilter(...) for partial indexes, IncludeProperties(...) for INCLUDE and IsCreatedConcurrently()."
---

| C# / SQL Server | PostgreSQL |
| --- | --- |
| nonclustered index with INCLUDE | B-tree with INCLUDE |
| filtered index (WHERE ...) | partial index |
| index on a computed column | expression index |
| full-text index | GIN on a tsvector |
| columnstore for large scans | BRIN on naturally ordered columns, or partitioning |
| HasIndex(x => x.Tags).HasMethod("gin") | CREATE INDEX ... USING gin (tags) |
