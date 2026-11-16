---
from: csharp
status: draft
concept: "Integration patterns and sagas"
from_way: "A `TransactionScope` around calls to several services, expecting a distributed transaction to save the day."
native_way: "MassTransit or NServiceBus sagas (state machines persisted with EF Core), Azure Service Bus dead letter queues, Durable Functions or Temporal for orchestration, and YARP as a gateway or BFF."
sensei_tip: "MassTransit's state machine sagas persist their state and correlate messages for you: a saga orchestrator without writing the bookkeeping."
---

| C# / .NET | Integration idea |
| --- | --- |
| MassTransit, NServiceBus | channels, routing, competing consumers |
| Azure Service Bus dead-letter sub-queue | dead letter channel |
| MassTransit state machine saga, Durable Functions | orchestrated saga |
| events between services | choreographed saga |
| YARP reverse proxy | API gateway, backend for frontend |
| `CorrelationId` on messages | correlation identifier |
