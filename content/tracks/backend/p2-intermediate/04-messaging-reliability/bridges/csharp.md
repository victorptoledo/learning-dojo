---
from: csharp
status: draft
concept: "Reliable events"
from_way: "MassTransit or NServiceBus, often with their outbox features, or raw clients."
native_way: "The patterns those libraries implement."
sensei_tip: "MassTransit's transactional outbox and inbox are this lesson's outbox and idempotent consumer; know what they do so you configure them correctly."
---

| C# | In practice |
| --- | --- |
| MassTransit outbox | transactional outbox |
| inbox / deduplication | idempotent consumer |
| retry policies | retries with backoff |
| _error queue | dead-letter queue |
