---
from: java
status: draft
concept: "Choosing status codes"
from_way: "Returning `ResponseEntity.ok(...)` for everything and putting failures in the body."
native_way: "`ResponseEntity.created(uri)`, `noContent()`, `notFound()`, `status(HttpStatus.CONFLICT)`, or exceptions mapped by `@RestControllerAdvice`."
sensei_tip: "`ResponseStatusException(HttpStatus.CONFLICT, \"...\")` is quick, but a typed domain exception plus one advice keeps status decisions in one place."
---

| Java / Spring | Status code |
| --- | --- |
| `ResponseEntity.created(uri).body(x)` | 201 + Location |
| `ResponseEntity.noContent().build()` | 204 |
| `ResponseEntity.of(optional)` | 200 or 404 |
| `HttpStatus.UNPROCESSABLE_CONTENT` | 422 |
| `HttpStatus.TOO_MANY_REQUESTS` | 429 |
