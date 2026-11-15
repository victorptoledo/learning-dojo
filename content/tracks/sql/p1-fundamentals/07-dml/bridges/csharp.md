---
from: csharp
status: draft
concept: "Creating, changing and removing stored records"
from_way: "EF Core change tracking with Add, Remove and SaveChanges, or ExecuteUpdate/ExecuteDelete for set-based changes."
native_way: "INSERT, UPDATE and DELETE statements, with RETURNING (PostgreSQL) or OUTPUT (SQL Server)."
sensei_tip: "EF Core 7+ ExecuteUpdateAsync and ExecuteDeleteAsync emit one set-based statement instead of loading entities: use them for bulk changes."
---

| C# | SQL |
| --- | --- |
| context.Add(c); SaveChanges() | INSERT ... RETURNING id |
| ExecuteUpdate(s => s.SetProperty(p => p.Price, p => p.Price * 1.1m)) | UPDATE products SET price = price * 1.1 |
| ExecuteDelete() | DELETE FROM ... WHERE ... |
| cmd.ExecuteNonQuery() rows affected | UPDATE 3 |
| OUTPUT inserted.Id (SQL Server) | RETURNING id |
