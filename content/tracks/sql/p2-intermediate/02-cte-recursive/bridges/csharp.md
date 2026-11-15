---
from: csharp
status: draft
concept: "Named intermediate results, recursion and stored queries"
from_way: "let clauses or local variables in LINQ, recursive methods over navigation properties, and reusable IQueryable methods."
native_way: "WITH, WITH RECURSIVE and views; EF Core can map an entity to a view."
sensei_tip: "EF Core cannot express recursion in LINQ; write the recursive CTE with FromSql and map the result to a keyless entity."
---

| C# | SQL |
| --- | --- |
| let paid = ... | WITH paid AS (...) |
| IEnumerable<Node> Walk(Node n) => n.Children.SelectMany(Walk).Prepend(n) | WITH RECURSIVE ... |
| HashSet<int> visited | a path array or CYCLE |
| modelBuilder.Entity<X>().ToView("paid_orders") | CREATE VIEW paid_orders AS ... |
| IMemoryCache | a materialized view |
