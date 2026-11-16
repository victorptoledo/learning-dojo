---
from: java
status: draft
concept: "Problem details (RFC 9457)"
from_way: "A custom `ErrorResponse {code, message}` class per service, or Spring's old `/error` JSON with `timestamp`, `path` and `trace`."
native_way: "`ProblemDetail` with `setType`, `setTitle`, `setInstance`, `setProperty`, returned from a `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler`."
sensei_tip: "Set `spring.mvc.problemdetails.enabled=true` (or extend `ResponseEntityExceptionHandler`) so Spring's own errors, such as malformed JSON and 405s, are problem details too."
---

| Java / Spring | Problem details |
| --- | --- |
| `ProblemDetail.forStatusAndDetail(...)` | a problem |
| `problem.setType(URI.create(...))` | type URI |
| `problem.setProperty("balance", 500)` | extension member |
| `handleMethodArgumentNotValid` override | validation errors |
| `ErrorResponseException` | an exception that carries its problem |
