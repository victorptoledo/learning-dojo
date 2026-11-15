---
from: javascript
status: draft
concept: "Reliable events"
from_way: "kafkajs or amqplib producers called right after the database write."
native_way: "Outbox, idempotency and retries designed together."
sensei_tip: "Consumers in kafkajs see messages at least once; store processed event ids in the same transaction as the side effect."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| await db.save(); await producer.send() | dual write |
| outbox table | events written with the data |
| processed_events table | idempotent consumer |
| retry topic / DLQ | poison message handling |
