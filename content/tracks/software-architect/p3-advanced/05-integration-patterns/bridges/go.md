---
from: go
status: draft
concept: "Integration patterns and sagas"
from_way: "A handler that calls three gRPC services in sequence and returns an error after the second, leaving the first done."
native_way: "NATS or Kafka for channels, dead letter subjects or topics, sagas orchestrated with Temporal's Go SDK, idempotency keys in every request, and a gateway (Envoy, Kong) or a small Go BFF in front."
sensei_tip: "Temporal was born in Go (as Uber's Cadence). Its workflows are ordinary Go functions whose state survives crashes, which is exactly what a saga orchestrator needs."
---

| Go | Integration idea |
| --- | --- |
| NATS subjects, Kafka topics | channels and pub-sub |
| a dead letter subject or topic | dead letter channel |
| Temporal Go SDK | orchestrated saga |
| events between services | choreographed saga |
| Envoy, Kong, a Go BFF | API gateway, backend for frontend |
| gRPC metadata with a request id | correlation identifier |
