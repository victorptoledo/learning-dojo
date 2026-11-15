---
from: python
status: draft
concept: "Automatic elasticity"
from_way: "A fixed number of Celery workers that is too many at night and too few on Monday morning."
native_way: "KEDA scales the worker Deployment on the queue length, and the HPA scales the web tier on CPU or requests per second."
sensei_tip: "KEDA has triggers for Redis lists and RabbitMQ queues, the usual Celery brokers: scale workers on queue length, not on CPU."
---

| Python world | Kubernetes |
| --- | --- |
| `celery worker --autoscale=10,3` inside one Pod | KEDA scaling Pods on the queue |
| gunicorn workers per host | replicas scaled by the HPA |
| a Redis queue length check | a KEDA `redis` trigger |
| a bigger VM | VPA or a bigger request |
