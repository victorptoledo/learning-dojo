---
from: csharp
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "LINQ Select with index, Aggregate for running totals, and GroupBy then Take for top-N per group."
native_way: "Window functions, which LINQ cannot express directly: write them in SQL and map the result with FromSql."
sensei_tip: "GroupBy(...).SelectMany(g => g.OrderByDescending(...).Take(2)) is top-N per group; in SQL it is row_number() in a CTE filtered outside."
---

| C# | SQL |
| --- | --- |
| list.Select((x, i) => i + 1) | row_number() OVER (ORDER BY ...) |
| running total with a mutable sum in Select | sum(x) OVER (ORDER BY day) |
| list.Zip(list.Skip(1)) | lag(x) / lead(x) |
| GroupBy(...).SelectMany(g => g.OrderByDescending(e => e.Salary).Take(2)) | row_number() OVER (PARTITION BY dept ORDER BY salary DESC) <= 2 |
