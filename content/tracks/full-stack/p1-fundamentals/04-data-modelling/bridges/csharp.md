---
from: csharp
status: reviewed
concept: "From EF Core models to page queries"
from_way: "Navigation properties with lazy loading, and a Razor page that loops over them."
native_way: "EF Core migrations reviewed in pull requests, Include or projections with Select for lists, keyset paging, and check constraints configured in the model."
sensei_tip: "Turn on EF Core's command logging and AsNoTracking for read pages; Select into a DTO avoids loading what the screen never shows."
---

| EF Core habit | Full stack practice |
| --- | --- |
| lazy loading proxies | `Include` or `Select` projections |
| `Skip(4000).Take(20)` | `Where(o => o.Id < cursor).Take(21)` |
| `EnsureCreated()` | `dotnet ef migrations add` + review |
| `[Range]` only | `HasCheckConstraint` in the model |
