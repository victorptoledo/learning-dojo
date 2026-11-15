---
from: go
status: draft
concept: "Architecture choices"
from_way: "Small services by default, with gRPC or HTTP between them."
native_way: "Services cut by capability, and patterns for consistency across them."
sensei_tip: "Go makes small services cheap to build, not cheap to operate. Each extra network hop needs timeouts, retries and observability."
---

| Go | In practice |
| --- | --- |
| many small binaries | services by capability |
| gRPC calls | synchronous coupling |
| NATS or Kafka | asynchronous events |
| 2PC | sagas instead |
