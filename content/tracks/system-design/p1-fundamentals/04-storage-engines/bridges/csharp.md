---
from: csharp
status: draft
concept: "How data is stored and found"
from_way: "Entity Framework Core migrations create tables and indexes; SQL Server does the rest."
native_way: "Choose a model and engine per access pattern, design composite and covering indexes on purpose, and move analytics to a columnar store."
sensei_tip: "SQL Server's clustered index is the table itself, sorted by key; columnstore indexes are its OLAP side. EF Core's HasIndex(...).IncludeProperties(...) builds covering indexes."
---

| C# / .NET | Storage concept |
| --- | --- |
| `HasIndex(o => new { o.CustomerId, o.CreatedAt })` | a composite index |
| `IncludeProperties(...)` | a covering index |
| clustered columnstore index | columnar storage for analytics |
| `Azure.Storage.Blobs` | object storage |
| `SortedDictionary` | a sorted index in memory |
