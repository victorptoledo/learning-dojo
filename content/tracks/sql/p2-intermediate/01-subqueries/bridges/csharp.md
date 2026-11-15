---
from: csharp
status: draft
concept: "Queries inside queries, and combining whole result sets"
from_way: "LINQ subqueries inside Where, Contains on another sequence, and Union, Intersect, Except and Concat."
native_way: "SQL subqueries and set operations; EF Core translates the LINQ forms directly."
sensei_tip: "LINQ Union, Intersect and Except remove duplicates like their SQL namesakes; Concat keeps them like UNION ALL."
---

| C# | SQL |
| --- | --- |
| emps.Where(e => e.Salary > emps.Where(x => x.Dept == e.Dept).Average(x => x.Salary)) | correlated subquery |
| ids.Contains(o.CustomerId) | IN (SELECT ...) |
| a.Union(b) | UNION |
| a.Intersect(b) | INTERSECT |
| a.Except(b) | EXCEPT |
| a.Concat(b) | UNION ALL |
