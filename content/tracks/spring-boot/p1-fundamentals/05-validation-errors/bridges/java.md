---
from: java
status: draft
concept: "Input checks and error responses"
from_way: "if/else checks at the top of every method, a custom ErrorDto per project, and catch blocks in every controller returning 500 with the exception message."
native_way: "Bean Validation constraints on input records with @Valid, domain exceptions translated in one @RestControllerAdvice, and RFC 9457 Problem Details."
sensei_tip: "Let validation reject shape problems (missing, too long, wrong format) and keep business rules (duplicates, limits) in the service, mapped to 409 or 422."
---

| Before | Spring Boot 4 |
| --- | --- |
| `if (req.getEmail() == null) return badRequest("email required")` | `@NotBlank @Email String email` + `@Valid` |
| `class ErrorDto { String code; String message; }` | `ProblemDetail` (`application/problem+json`) |
| `try { ... } catch (Exception e) { return 500 }` in each controller | `@ExceptionHandler` in a `@RestControllerAdvice` |
| `javax.validation` | `jakarta.validation` |
