---
from: go
status: draft
concept: "Microservices"
from_way: "Many small Go binaries split by noun, sharing one Postgres database and one `models` module, calling each other in synchronous chains."
native_way: "Services that own their data, contracts in protobuf (gRPC) or OpenAPI, `context` deadlines on every call, NATS or Kafka for asynchronous work, and OpenTelemetry tracing."
sensei_tip: "Pass `context.Context` with a deadline through every call: it is Go's built-in way to make a whole chain of services give up together instead of piling up goroutines."
---

| Go | Microservice concern |
| --- | --- |
| a binary with its own database | independent deployability, data ownership |
| `context.WithTimeout`, retries with backoff | partial failure |
| NATS, `franz-go` for Kafka | asynchronous integration |
| protobuf with `buf breaking` | contract compatibility |
| OpenTelemetry Go | tracing across services |
| a shared `models` Go module | distributed monolith warning |
