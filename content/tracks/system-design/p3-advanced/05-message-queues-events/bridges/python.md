---
from: python
status: draft
concept: "Talking through messages"
from_way: "Celery tasks on Redis or RabbitMQ for background jobs."
native_way: "Queues for tasks, logs for event streams consumed by many groups; keys define order, consumers are idempotent."
sensei_tip: "Celery is a task queue: perfect for jobs, wrong for an event stream several services must read and replay. For that, confluent-kafka's Producer.produce(topic, key=..., value=...) and one group id per consumer service."
---

| Python | Messaging concept |
| --- | --- |
| `@app.task(acks_late=True)` (Celery) | at-least-once task processing |
| `Producer.produce("orders", key=order_id, value=...)` | keyed records, ordered per key |
| `Consumer({"group.id": "billing"})` | a consumer group |
| `consumer.commit(asynchronous=False)` | committing offsets after processing |
| Faust / Bytewax | stream processing in Python |
