---
from: java
status: draft
concept: "Event-driven architecture and the outbox"
from_way: "`@Transactional` methods that save an entity and call `kafkaTemplate.send(...)` in the same method, assuming both succeed together."
native_way: "An outbox table written in the same transaction (or Spring Modulith's event publication registry), a relay or Debezium publishing to Kafka, consumers that record processed event ids, and events as versioned records or Avro schemas."
sensei_tip: "`kafkaTemplate.send` inside `@Transactional` is not part of the database transaction. A rollback after the send leaves a ghost event; a crash after the commit loses it. That is the dual write."
---

| Java | Event-driven idea |
| --- | --- |
| an `outbox` table and a scheduled relay | transactional outbox |
| Debezium outbox event router | outbox via change data capture |
| Spring Modulith event publication registry | outbox for in-process events |
| `@KafkaListener` with a processed-ids table | idempotent consumer |
| Axon Framework, EventStoreDB client | event sourcing |
| a separate read-model table fed by listeners | CQRS read model |
