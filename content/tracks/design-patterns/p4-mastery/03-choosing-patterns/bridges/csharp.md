---
from: csharp
status: draft
concept: "Choosing patterns"
from_way: "Generic repositories, unit-of-work wrappers around EF Core and interfaces for every class."
native_way: "Choosing from the forces and using what .NET provides: delegates, records and pattern matching, the DI container, middleware and `DelegatingHandler` chains, EF Core as repository and unit of work, MediatR only where a mediator helps."
sensei_tip: "In .NET, the most common pattern to remove is the generic repository over EF Core: `DbContext` already is a unit of work and `DbSet` a repository."
---

| Problem | C# choice |
| --- | --- |
| varying algorithm | `Func<>` delegates, keyed services |
| cross-cutting behaviour | middleware, `DelegatingHandler`, filters |
| closed variants | records + switch expressions |
| reacting to facts | events, `INotification` handlers |
| foreign model | adapter in Infrastructure |
| combinable rules | `Expression<Func<T, bool>>` |
