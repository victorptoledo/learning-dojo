---
from: java
status: draft
concept: "API failure modes in production"
from_way: "Default timeouts (often infinite), retries in Resilience4j, Feign and the service mesh at the same time."
native_way: "Explicit connect and read timeouts, one retrying layer with a retry budget, circuit breakers and bulkheads from Resilience4j, Micrometer metrics tagged by client and operation."
sensei_tip: "Micrometer's `http.server.requests` metric is tagged by `uri` and `status`; add a client tag (API key id, not the key) to see one partner failing."
---

| Java / Spring | Production concern |
| --- | --- |
| `RestClient` with explicit timeouts | bounded calls |
| Resilience4j `CircuitBreaker`, `Bulkhead` | stop cascades |
| one retry layer + jitter | no retry storms |
| Micrometer tags per client | per-consumer observability |
| Spring Boot `management.tracing` | correlation ids |
