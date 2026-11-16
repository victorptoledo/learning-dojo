---
from: csharp
status: draft
concept: "Tactical DDD"
from_way: "EF Core entities with public setters and navigation properties to everything, and rules in application services."
native_way: "`record struct` or `record` value objects, strongly typed ids, aggregate roots with private setters and methods, a repository (or `DbSet`) per root, owned types for values, and domain events raised by the root."
sensei_tip: "EF Core maps value objects as owned types (`OwnsOne`) and private backing fields for collections, so a rich aggregate with no public setters persists fine."
---

| C# / .NET | Tactical DDD idea |
| --- | --- |
| `record Money(long Cents, string Currency)` | value object |
| `readonly record struct OrderId(Guid Value)` | identity as a value |
| a class with `private set` and methods | aggregate root |
| one repository or `DbSet` per root | repository per aggregate |
| `AddDomainEvent(...)`, dispatched after `SaveChanges` | domain events |
| `[ConcurrencyCheck]`, row version | concurrency on the aggregate boundary |
