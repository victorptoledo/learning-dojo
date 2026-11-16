---
from: javascript
status: draft
concept: "Event-driven architecture and the outbox"
from_way: "`await db.order.create(...)` followed by `await producer.send(...)` in a route handler, with no plan for a crash in between."
native_way: "An outbox row created in the same `prisma.$transaction` as the change, a relay worker publishing to Kafka or SQS, consumers that record processed event ids, and partitions keyed by aggregate id."
sensei_tip: "Two `await`s in a row are not atomic. If the process is killed between them, the second never runs; an outbox row written inside the same transaction is the only safe hand-off."
---

| JavaScript / TypeScript | Event-driven idea |
| --- | --- |
| an outbox table in the same `$transaction` | transactional outbox |
| a relay worker, Debezium | outbox relay |
| KafkaJS consumer with a processed-ids table | idempotent consumer |
| SQS FIFO deduplication id | broker-side deduplication (limited window) |
| EventStoreDB Node client | event sourcing |
| read-model tables fed by consumers | CQRS read model |
