---
from: python
status: draft
concept: "Microservices"
from_way: "Several Flask or FastAPI services that import the same SQLAlchemy models package and share one database."
native_way: "Services with their own database and models, contracts in OpenAPI or JSON Schema, `httpx` with timeouts and retries, Celery or Kafka for asynchronous work, and OpenTelemetry for tracing."
sensei_tip: "Python's GIL and per-process workers often make 'scale this one endpoint' the first reason teams split a service. A worker pool or a separate deployment of the same codebase may solve it without a new service."
---

| Python | Microservice concern |
| --- | --- |
| a FastAPI service with its own database | independent deployability, data ownership |
| `httpx` with timeouts, `tenacity` retries | partial failure |
| Celery, `aiokafka`, `faststream` | asynchronous integration |
| Pact, schemathesis | contract tests |
| OpenTelemetry | tracing across services |
| a shared models package | distributed monolith warning |
