---
from: java
status: draft
concept: "Microservices"
from_way: "Splitting a Spring Boot monolith into services that share a `common-model` jar of JPA entities and call each other with `RestTemplate` in long chains."
native_way: "Services that own their data and schema, publish contracts (OpenAPI, Avro or JSON events) instead of shared entity jars, use timeouts and resilience (Resilience4j), and communicate asynchronously where no answer is needed."
sensei_tip: "A shared jar of domain classes across services forces lock-step upgrades: the hallmark of a distributed monolith. Share contracts, generate code from them, and let each service own its model."
---

| Java | Microservice concern |
| --- | --- |
| Spring Boot per service, its own database | independent deployability, data ownership |
| `RestClient` with timeouts, Resilience4j | partial failure |
| Spring Cloud Stream, Kafka | asynchronous integration |
| Spring Cloud Contract, Pact | contract tests |
| Micrometer Tracing, OpenTelemetry | tracing across services |
| a shared `common-model` jar | distributed monolith warning |
