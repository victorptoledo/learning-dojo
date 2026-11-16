---
from: java
status: draft
concept: "HTTP methods, safety and idempotency"
from_way: "`@PostMapping` for everything because it \"always works\", and `@GetMapping` on endpoints that change data."
native_way: "`@GetMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` chosen by semantics; `produces`/`consumes` declare the media types."
sensei_tip: "Spring answers 406 and 415 for you when `produces` and `consumes` are declared on the mapping, and 405 with an `Allow` header for unmapped methods."
---

| Java / Spring | HTTP concept |
| --- | --- |
| `@GetMapping` | safe, idempotent |
| `@PutMapping` | idempotent full replacement |
| `@PatchMapping(consumes = "application/merge-patch+json")` | partial update |
| `@PostMapping` | neither safe nor idempotent |
| `produces = MediaType.APPLICATION_JSON_VALUE` | content negotiation |
