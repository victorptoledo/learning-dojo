---
from: csharp
status: draft
concept: "Event-driven architecture and the outbox"
from_way: "`SaveChangesAsync()` followed by `bus.Publish(...)` in an application service."
native_way: "The transactional outbox built into MassTransit or NServiceBus with EF Core, consumers with the inbox (deduplication) pattern, and Marten or EventStoreDB for event sourcing."
sensei_tip: "MassTransit's EF Core outbox and inbox give you the outbox relay and idempotent consumers as configuration. Turn them on before the first production incident, not after."
---

| C# / .NET | Event-driven idea |
| --- | --- |
| MassTransit `AddEntityFrameworkOutbox` | transactional outbox |
| NServiceBus outbox | outbox and deduplication |
| inbox (processed message ids) | idempotent consumer |
| Marten, EventStoreDB | event sourcing |
| Marten projections, read-model tables | CQRS read model |
| MediatR commands and queries | CQRS inside one process |
