---
from: csharp
status: draft
concept: "Talking through messages"
from_way: "In-process events, or Azure Service Bus queues for background work."
native_way: "Queues with competing consumers for tasks, logs with consumer groups for events; keys define order, consumers are idempotent."
sensei_tip: "Azure Service Bus sessions (SessionId) give ordered processing per key on a queue, the queue-world answer to Kafka partition keys. MassTransit adds sagas and outbox support on top of several brokers."
---

| C# / .NET | Messaging concept |
| --- | --- |
| Service Bus `SessionId` | ordering per key on a queue |
| Confluent.Kafka `ProduceAsync(topic, new Message<K,V> { Key = id })` | keyed records |
| `ConsumerConfig { GroupId = "billing" }` | a consumer group |
| Service Bus dead-letter sub-queue | dead letters |
| MassTransit state machine sagas | orchestration with compensation |
