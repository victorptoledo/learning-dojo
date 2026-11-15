---
from: csharp
status: draft
concept: "Architecture choices"
from_way: "Clean Architecture solutions, modular monoliths and microservices with MassTransit."
native_way: "The same choices driven by bounded contexts and team boundaries."
sensei_tip: "MassTransit's state machines (sagas) implement the orchestration in this lesson; use them only when a business transaction really spans services."
---

| C# | In practice |
| --- | --- |
| projects per layer | modules per capability |
| MassTransit saga state machine | orchestrated saga |
| shared EF DbContext | data owned by one module |
| gRPC between services | a synchronous dependency to keep few |
