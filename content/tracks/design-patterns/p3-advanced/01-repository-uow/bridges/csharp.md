---
from: csharp
status: draft
concept: "Repository and unit of work"
from_way: "Controllers using `DbContext` directly, or a generic `IRepository<T>` wrapped around EF Core."
native_way: "EF Core's `DbContext` is already a unit of work and its `DbSet<T>` a repository; add domain-specific repository interfaces only where they hide real complexity."
sensei_tip: "Wrapping `DbContext` in a generic `IRepository<T>` and `IUnitOfWork` usually hides EF Core's features without adding anything; prefer specific repositories per aggregate, or use `DbContext` in application services."
---

| C# | Pattern |
| --- | --- |
| `DbContext` + `SaveChanges()` | unit of work |
| `DbSet<T>` | repository (generic) |
| change tracker | dirty checking, identity map |
| `IAccountRepository` per aggregate | domain repository |
| Dapper queries | table data gateway |
