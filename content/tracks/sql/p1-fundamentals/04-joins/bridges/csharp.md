---
from: csharp
status: draft
concept: "Combining related records"
from_way: "LINQ join ... on ... equals, GroupJoin with DefaultIfEmpty for left joins, and Include in EF Core."
native_way: "JOIN ... ON and LEFT JOIN: the SQL LINQ was designed after."
sensei_tip: "In LINQ the left join is GroupJoin plus DefaultIfEmpty (or the newer LeftJoin in .NET 10); in SQL it is just LEFT JOIN. Any() becomes EXISTS."
---

| C# | SQL |
| --- | --- |
| join c in customers on o.CustomerId equals c.Id | JOIN customers c ON c.id = o.customer_id |
| GroupJoin(...).SelectMany(x => x.DefaultIfEmpty()) | LEFT JOIN |
| orders.Any(o => o.CustomerId == c.Id) | EXISTS (...) |
| !orders.Any(...) | NOT EXISTS (...) |
| from a in xs from b in ys | CROSS JOIN |
