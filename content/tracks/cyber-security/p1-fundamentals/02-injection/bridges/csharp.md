---
from: csharp
status: draft
concept: "Keep code and data apart"
from_way: "SqlCommand with string concatenation and FromSqlRaw with interpolated input."
native_way: "SqlParameter or Dapper parameters, EF Core FromSql with interpolation (which parameterises), and ProcessStartInfo.ArgumentList."
sensei_tip: "In EF Core, `FromSql($\"... {x}\")` creates a parameter from the interpolation, while `FromSqlRaw` with a pre-built string does not: the difference is one method name."
---

| C# | Injection-safe way |
| --- | --- |
| new SqlCommand("..." + name) | cmd.Parameters.AddWithValue("@name", name) |
| FromSqlRaw(built string) | FromSql($"... {name}") |
| Process.Start("cmd", "/c ping " + host) | ProcessStartInfo.ArgumentList.Add(host) |
| Path.Combine(base, name) | Path.GetFullPath then StartsWith(base) |
| dynamic ORDER BY | an allow-list of columns |
