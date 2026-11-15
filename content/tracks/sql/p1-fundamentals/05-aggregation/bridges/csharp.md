---
from: csharp
status: draft
concept: "Summarising groups of records"
from_way: "LINQ GroupBy with Count, Sum, Average, and a Where on the groups."
native_way: "GROUP BY, aggregates and HAVING; EF Core translates a LINQ GroupBy to exactly that."
sensei_tip: "A Where after GroupBy becomes HAVING in the SQL EF Core generates; a Where before it becomes WHERE. Check the generated SQL to see which you wrote."
---

| C# | SQL |
| --- | --- |
| .GroupBy(o => o.CustomerId).Select(g => new { g.Key, Count = g.Count() }) | SELECT customer_id, count(*) ... GROUP BY customer_id |
| g.Sum(o => o.Total) | sum(total) |
| .Where(g => g.Count() > 5) | HAVING count(*) > 5 |
| g.Count(o => o.Cancelled) | count(*) FILTER (WHERE cancelled) |
| string.Join(", ", names) | string_agg(name, ', ') |
