---
from: java
status: draft
concept: "Evolving an API without breaking clients"
from_way: "Changing a DTO's field in place and bumping the whole service to `/v2`."
native_way: "Additive changes on the same DTO, `@Deprecated` fields kept during expand-then-contract, a separate controller (or DTO) for a real v2, and consumer contract tests (Pact, Spring Cloud Contract)."
sensei_tip: "Spring Boot's Jackson ignores unknown properties by default (`FAIL_ON_UNKNOWN_PROPERTIES` off): your clients in Java are tolerant readers unless someone turns it back on."
---

| Java / Spring | Evolution concept |
| --- | --- |
| new optional DTO field | compatible change |
| `@Deprecated` + `deprecated: true` in OpenAPI | expand, then contract |
| `@RequestMapping("/v2/customers")` | path version |
| `FAIL_ON_UNKNOWN_PROPERTIES = false` | tolerant reader |
| Pact / Spring Cloud Contract | consumer contract tests |
