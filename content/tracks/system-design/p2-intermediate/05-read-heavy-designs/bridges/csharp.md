---
from: csharp
status: draft
concept: "Making reads cheap"
from_way: "EF Core Include and ThenInclude building view models from many tables per request."
native_way: "Write-side models for commands, read-side projections for screens, inboxes for feeds, and keyset pagination."
sensei_tip: "MediatR-style command and query handlers are often called CQRS, but the real gain comes from separate read models. Start with a projection table updated by an event handler for your slowest screen."
---

| C# / .NET | Read-heavy concept |
| --- | --- |
| `Include(...).ThenInclude(...)` | joins at read time |
| a projection table updated by a handler | a CQRS read model |
| `Where(p => p.Id < lastId).Take(20)` | keyset pagination |
| `Interlocked.Increment` | an in-memory counter, batched later |
| StackExchange.Redis `SortedSetAdd` | a per-user inbox |
