---
from: python
status: draft
concept: "Integration patterns and sagas"
from_way: "A view that calls three services with `requests` one after another and leaves the first two done when the third fails."
native_way: "Celery, Kafka or RabbitMQ with dead letter queues, sagas orchestrated with Temporal's Python SDK or a state machine, idempotency keys on every step, and an API gateway in front."
sensei_tip: "Celery's `acks_late` plus retries means a task can run twice. Give every task an idempotency key before you rely on retries."
---

| Python | Integration idea |
| --- | --- |
| Celery, Dramatiq, `aio-pika` | channels and competing consumers |
| dead letter exchanges in RabbitMQ | dead letter channel |
| Temporal Python SDK | orchestrated saga |
| events between services | choreographed saga |
| idempotency keys stored per request | idempotent receiver |
| a FastAPI service per client | backend for frontend |
