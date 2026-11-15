---
from: csharp
status: draft
concept: "SQL behind application code"
from_way: "ADO.NET parameters, Dapper, Entity Framework Core, and EF Core migrations."
native_way: "Bind parameters, eager loading, keyset pagination and migrations."
sensei_tip: "FromSqlInterpolated and FromSql with interpolated strings bind parameters; FromSqlRaw with concatenation does not. Dapper's anonymous objects bind too."
---

| C# | SQL |
| --- | --- |
| cmd.Parameters.AddWithValue("@email", email) | a bind parameter |
| FromSqlRaw("... " + name) | SQL injection |
| Include(o => o.Customer) | a join instead of N+1 |
| .Where(o => o.Id > lastId).OrderBy(o => o.Id).Take(20) | keyset pagination |
| dotnet ef migrations add | a versioned migration |
