---
from: csharp
status: draft
concept: "Filtering, projecting and sorting a collection"
from_way: "LINQ: Where, Select, OrderBy/ThenBy, Distinct and Take."
native_way: "The same operations as SQL clauses: WHERE, SELECT, ORDER BY, DISTINCT, FETCH FIRST."
sensei_tip: "LINQ method order is free, SQL clause order is fixed. EF Core translates Take(5) to LIMIT on PostgreSQL and TOP on SQL Server."
---

| C# | SQL |
| --- | --- |
| .Where(p => p.Price < 20) | WHERE price < 20 |
| .Select(p => p.Name) | SELECT name |
| .OrderBy(p => p.Price).ThenBy(p => p.Name) | ORDER BY price, name |
| .Distinct() | SELECT DISTINCT |
| .Take(5) | FETCH FIRST 5 ROWS ONLY |
| p.Price switch { < 10 => "cheap", _ => "premium" } | CASE WHEN price < 10 THEN 'cheap' ELSE 'premium' END |
