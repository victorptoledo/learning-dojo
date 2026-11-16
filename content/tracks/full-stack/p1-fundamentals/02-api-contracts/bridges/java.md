---
from: java
status: reviewed
concept: "From DTOs to a contract the front end can compile against"
from_way: "Java records as DTOs on the server and hand-written TypeScript interfaces in the SPA that drift apart."
native_way: "springdoc generates OpenAPI from the controllers in CI, the front end generates its types and client from it, and Bean Validation plus ProblemDetail shape the errors."
sensei_tip: "Fail the build when the generated OpenAPI changes without the client being regenerated: that single check catches most contract drift."
---

| Spring | Contract |
| --- | --- |
| `record ProductView(...)` | a schema in `openapi.yaml` |
| `@Valid @RequestBody` + `@NotBlank` | server-side validation |
| `ProblemDetail.forStatusAndDetail(...)` | RFC 9457 problem details |
| `springdoc-openapi` in CI | generated TypeScript types for the UI |
