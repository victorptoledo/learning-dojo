---
from: csharp
status: draft
concept: "Access paths and the plan that uses them"
from_way: "Dictionary<TKey, TValue> and SortedList for lookups, and dotTrace or the VS profiler."
native_way: "Indexes and query plans; SQL Server Management Studio and psql both show them."
sensei_tip: "Declare indexes in EF Core with HasIndex(o => new { o.CustomerId, o.PlacedAt }); then check the plan, because a LINQ .Date or ToLower() on a column produces a non-sargable WHERE."
---

| C# | SQL |
| --- | --- |
| Dictionary<long, Order> | the primary key index |
| HasIndex(o => new { o.CustomerId, o.PlacedAt }) | a composite index |
| Where(o => o.PlacedAt.Date == day) | date_trunc in WHERE: write a range |
| HasFilter("status = 'pending'") | a partial index |
| a profiler | EXPLAIN ANALYZE |
