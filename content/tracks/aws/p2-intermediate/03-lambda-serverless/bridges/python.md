---
from: python
status: draft
concept: "Functions triggered by events, connected by managed services"
from_way: "Flask serves requests and Celery workers with a Redis broker do the background work."
native_way: "API Gateway calls a Lambda handler; SQS replaces the broker; EventBridge routes events; Step Functions chains steps."
sensei_tip: "Use Powertools for AWS Lambda: `Logger`, `Tracer`, `Metrics`, `@idempotent` and `BatchProcessor` for SQS partial failures save a lot of hand-written code."
---

| Python world | AWS |
| --- | --- |
| a Flask route | an HTTP API route to a function |
| a Celery task | an SQS message and a worker function |
| Celery beat | EventBridge Scheduler |
| a Celery chain | a Step Functions state machine |
