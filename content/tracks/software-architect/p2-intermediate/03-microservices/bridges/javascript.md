---
from: javascript
status: draft
concept: "Microservices"
from_way: "A Node.js service per route group, all importing the same Prisma client and schema, calling each other with `fetch` and no timeouts."
native_way: "Services with their own schema, contracts in OpenAPI or a schema registry, `fetch` with `AbortSignal.timeout`, queues or Kafka between services, and OpenTelemetry tracing."
sensei_tip: "`fetch` has no timeout by default: a slow dependency holds sockets and memory until the caller falls over. Add `AbortSignal.timeout(ms)` to every call between services."
---

| JavaScript / TypeScript | Microservice concern |
| --- | --- |
| a Node.js service with its own database | independent deployability, data ownership |
| `AbortSignal.timeout`, `opossum` circuit breaker | partial failure |
| BullMQ, KafkaJS, SNS and SQS | asynchronous integration |
| Pact, generated OpenAPI clients | contract tests |
| OpenTelemetry for Node.js | tracing across services |
| one Prisma schema shared by all services | distributed monolith warning |
