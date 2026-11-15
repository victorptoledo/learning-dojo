---
from: csharp
status: draft
concept: "Queries that look right and are not"
from_way: "Pitfalls like multiple enumeration of IQueryable, client evaluation and Include explosions."
native_way: "SQL pitfalls: fan-out, DISTINCT bandages, non-sargable filters, missing ORDER BY."
sensei_tip: "Several Include() of collections in one query is a fan-out (a cartesian explosion); EF Core's AsSplitQuery() runs one query per collection instead."
---

| C# | SQL |
| --- | --- |
| Include(a => a.Orders).Include(a => a.Tickets) | fan-out: rows multiplied |
| AsSplitQuery() | aggregate or load each child separately |
| Any() vs Count() > 0 | EXISTS vs count(*) |
| double for money | numeric (decimal) |
| Where(x => x.Date.ToString("yyyy-MM") == m) | non-sargable: use a range |
