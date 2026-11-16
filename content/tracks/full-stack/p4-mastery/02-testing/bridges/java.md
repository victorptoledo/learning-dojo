---
from: java
status: reviewed
concept: "From mocked repositories to Testcontainers"
from_way: "Mockito mocks for every repository, an H2 database that behaves differently from production, and no tests of the UI at all."
native_way: "@SpringBootTest or slices with Testcontainers PostgreSQL, MockMvc for HTTP and security, contract checks against the generated OpenAPI, and a few Playwright journeys."
sensei_tip: "Spring Boot's @ServiceConnection on a Testcontainers bean wires the datasource for you; there is little reason left to test on H2."
---

| Java | Across the stack |
| --- | --- |
| `@Mock Repository` | Testcontainers PostgreSQL |
| `MockMvc` | the HTTP and security contract |
| Spring Cloud Contract or Pact JVM | consumer-driven contracts |
| Playwright for Java | end-to-end journeys |
