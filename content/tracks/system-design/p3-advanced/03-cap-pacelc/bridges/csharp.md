---
from: csharp
status: draft
concept: "What a read is allowed to return"
from_way: "SQL Server transactions on one machine, where isolation levels decide what a reader sees."
native_way: "Consistency levels across replicas and regions, chosen per request, trading latency and availability."
sensei_tip: "Azure Cosmos DB exposes five consistency levels (strong, bounded staleness, session, consistent prefix, eventual): a product built directly on this lesson. Session is the default because it gives read-your-writes cheaply."
---

| C# / .NET | Distributed consistency |
| --- | --- |
| Cosmos DB `ConsistencyLevel.Session` | read-your-writes per session |
| Cosmos DB `ConsistencyLevel.Strong` | linearizable, cross-region latency |
| `IsolationLevel.Serializable` | strong guarantees on one database |
| ETag with `If-Match` | a conditional write |
| `ConcurrentDictionary` | one-machine linearizability |
