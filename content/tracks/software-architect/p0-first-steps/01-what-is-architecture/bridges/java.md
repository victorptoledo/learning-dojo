---
from: java
status: draft
concept: "Software architecture"
from_way: "Architecture means the framework stack: a Spring Boot app with controller, service and repository packages, decided once by a template."
native_way: "Architecture as the decisions that are expensive to change (module boundaries, data ownership, synchronous or asynchronous integration, deployment units), each driven by quality attributes and recorded with its reasons."
sensei_tip: "Spring Boot makes many choices for you (embedded server, auto-configuration, one deployable). They are architectural decisions too: know which ones you accepted and what they cost."
---

| Java world | Architectural question behind it |
| --- | --- |
| one Spring Boot jar or several | deployment units, availability, team independence |
| Maven or Gradle modules, JPMS | module structure, what may depend on what |
| JPA entities shared by every package | data ownership and coupling |
| `@Async`, Kafka listeners | synchronous versus event-driven runtime structure |
| Actuator, Micrometer | observability as a quality attribute |
| JMH benchmarks, Gatling load tests | checking performance scenarios |
