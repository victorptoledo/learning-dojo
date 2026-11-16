---
from: csharp
status: draft
concept: "Modular monolith"
from_way: "One ASP.NET Core solution with a single `DbContext` that maps every table, used by every feature."
native_way: "One project (or a few) per module with `internal` implementation and a public contracts project, one `DbContext` and schema per module, MediatR notifications or an in-process bus between modules, and NetArchTest rules."
sensei_tip: "Give each module its own `DbContext` with its own schema (`modelBuilder.HasDefaultSchema(\"orders\")`). A single context for everything is the shared-database coupling of a ball of mud."
---

| C# / .NET | Modular monolith idea |
| --- | --- |
| `Orders.Contracts` and `Orders` projects | published interface and hidden module |
| `internal` types | hidden implementation |
| one `DbContext` per module | data ownership |
| MediatR `INotification`, an in-memory bus | in-process events |
| NetArchTest, ArchUnitNET | boundaries as tests |
| one host project referencing all modules | composition root |
