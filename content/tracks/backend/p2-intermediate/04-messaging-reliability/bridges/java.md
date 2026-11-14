---
from: java
status: draft
concept: "Reliable events"
from_way: "Calling kafkaTemplate.send() right after repository.save(), consumers that add to counters on every delivery, and failed messages retried in a tight loop."
native_way: "An outbox row written in the same transaction, a relay that publishes it, consumers that deduplicate by event id, retries with backoff and a dead-letter queue."
sensei_tip: "Assume every message arrives twice and some arrive late; if your consumer is correct under that assumption, most messaging bugs disappear."
---

| Habit | Reliable |
| --- | --- |
| `save(order); kafka.send(event);` | `save(order); outbox.save(event);` in one transaction |
| `counter += event.amount()` | skip events whose id was already processed |
| `@Retryable(maxAttempts = Integer.MAX_VALUE)` | a few attempts with backoff, then the DLQ |
| One partition key for everything | the entity id as the key |
