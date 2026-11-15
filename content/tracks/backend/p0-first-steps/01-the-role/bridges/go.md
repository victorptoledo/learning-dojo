---
from: go
status: draft
concept: "Beyond the framework"
from_way: "Small services with net/http and explicit code, often close to the infrastructure already."
native_way: "The same view, made systematic: protocols, data, reliability and operations."
sensei_tip: "Go developers often see the plumbing already. The backend track adds the vocabulary and patterns (idempotency, outbox, SLOs) that make those systems dependable."
---

| Go | In practice |
| --- | --- |
| http.Handler | one step of the request |
| database/sql pool | connection limits and timeouts |
| log package | structured logs and traces |
| one binary | one of many replicas |
