---
from: python
status: draft
concept: "HTTP endpoints in Spring MVC"
from_way: "FastAPI path operations with Pydantic models, or Django REST framework views and serializers."
native_way: "@RestController classes with @GetMapping and friends, records for requests and responses, and Jackson for JSON."
sensei_tip: "A FastAPI function with a Pydantic body is a controller method with a record parameter. Spring returns 200 by default; use ResponseEntity for 201 with a Location header."
---

| Python | Spring Boot 4 |
| --- | --- |
| @app.get('/books/{id}') | @GetMapping("/books/{id}") |
| def get(id: int) | Book get(@PathVariable long id) |
| body: BookIn (Pydantic) | @RequestBody BookRequest request |
| status_code=201 | ResponseEntity.created(uri).body(...) |
| HTTPException(404) | ResponseStatusException or an advice |
