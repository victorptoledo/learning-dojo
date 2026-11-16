---
from: python
status: draft
concept: "Event-driven architecture and the outbox"
from_way: "Saving a Django model and calling `producer.send(...)` or `task.delay(...)` right after, trusting both to succeed."
native_way: "An outbox table written in the same database transaction, a relay process or Debezium publishing it, consumers that store processed event ids, and `transaction.on_commit` at the very least for in-process work."
sensei_tip: "Django's `transaction.on_commit(lambda: task.delay(...))` avoids the ghost event of a rolled-back transaction, but still loses the task if the process dies right after the commit. Only an outbox closes both holes."
---

| Python | Event-driven idea |
| --- | --- |
| an outbox model saved in the same `atomic()` block | transactional outbox |
| `transaction.on_commit` | publish only after commit (not crash-safe) |
| a relay worker or Debezium | outbox relay |
| a processed-ids table checked by consumers | idempotent consumer |
| `eventsourcing` library | event sourcing |
| read-model tables updated by consumers | CQRS read model |
