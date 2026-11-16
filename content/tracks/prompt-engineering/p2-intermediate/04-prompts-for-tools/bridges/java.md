---
from: java
status: draft
concept: "Tools as a public API for the model"
from_way: "A REST controller with an OpenAPI description, request DTOs with Bean Validation, and @PreAuthorize on the methods."
native_way: "Tool definitions are an API the model calls: descriptive names, documented parameters, a JSON Schema, validation on every call and permission checks in the tool."
sensei_tip: "Write tool descriptions like the summary and description of an OpenAPI operation for a new client team: when to call it, what it returns, and what errors mean."
---

| Java / Spring | Tool-use concept |
| --- | --- |
| an OpenAPI operation | a tool definition |
| `@Valid` on a request DTO | validating tool arguments |
| a `ProblemDetail` with field errors | an error result the model can act on |
| `@PreAuthorize` | permission checks in the tool's code |
| an idempotency key header | making repeated calls safe |
