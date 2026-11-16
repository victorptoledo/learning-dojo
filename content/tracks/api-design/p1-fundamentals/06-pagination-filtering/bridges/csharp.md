---
from: csharp
status: draft
concept: "Pagination, filtering and sorting"
from_way: "`.Skip(page * size).Take(size)` on an `IQueryable` for every collection."
native_way: "Keyset filtering in LINQ (rows strictly before the cursor's `(CreatedAt, Id)`) with an index on both columns, and an opaque cursor type."
sensei_tip: "EF Core translates the tuple-style keyset condition to an index seek only if the index columns and order match the `OrderBy`/`ThenBy` exactly."
---

| C# / ASP.NET Core | Collection concept |
| --- | --- |
| `Skip` / `Take` | offset |
| keyset `Where` + `OrderByDescending().ThenByDescending()` | cursor |
| `[Range(1, 100)] int limit` | maximum limit |
| `Base64Url.EncodeToString` | opaque cursor |
| `Response.Headers.Link` | Link header |
