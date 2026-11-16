---
from: go
status: draft
concept: "Event-driven architecture and the outbox"
from_way: "A handler that runs `tx.Commit()` and then `producer.Produce(...)`, logging the error if the second fails."
native_way: "An outbox row inserted in the same `sql.Tx`, a relay goroutine or separate process (or Debezium) publishing to Kafka or NATS, and consumers that record processed event ids in their own transaction."
sensei_tip: "Logging a failed publish after a commit is losing the event with a paper trail. Insert the outbox row before `tx.Commit()` and let a relay retry."
---

| Go | Event-driven idea |
| --- | --- |
| outbox `INSERT` inside the same `*sql.Tx` | transactional outbox |
| a relay goroutine with a ticker, or Debezium | outbox relay |
| a processed-ids table, `INSERT ... ON CONFLICT DO NOTHING` | idempotent consumer |
| Kafka keys by aggregate id | ordering per aggregate |
| NATS JetStream message deduplication | broker-side deduplication (limited window) |
| read-model tables fed by consumers | CQRS read model |
