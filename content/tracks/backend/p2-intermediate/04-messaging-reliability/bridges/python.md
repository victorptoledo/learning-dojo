---
from: python
status: draft
concept: "Reliable events"
from_way: "Saving to the database and then publishing to RabbitMQ or Kafka in the same request."
native_way: "A transactional outbox, at-least-once delivery, idempotent consumers and dead-letter queues."
sensei_tip: "Publishing after commit can lose the event if the process dies in between; writing the event to an outbox table in the same transaction cannot."
---

| Python | In practice |
| --- | --- |
| db.commit(); producer.send(event) | the dual-write problem |
| outbox table + relay | reliable publishing |
| consumer with processed ids | idempotent consumer |
| Celery retries | retries with backoff and a DLQ |
