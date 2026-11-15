---
from: csharp
status: draft
concept: "HTTP endpoints in Spring MVC"
from_way: "ASP.NET Core controllers with [HttpGet] or minimal APIs with app.MapGet."
native_way: "Spring MVC controllers with @GetMapping and records."
sensei_tip: "[ApiController] with [HttpGet(\"{id}\")] is @RestController with @GetMapping(\"/{id}\"); CreatedAtAction is ResponseEntity.created(uri)."
---

| C# | Spring Boot 4 |
| --- | --- |
| [ApiController] | @RestController |
| [HttpGet("{id}")] | @GetMapping("/{id}") |
| [FromBody] BookRequest | @RequestBody BookRequest |
| CreatedAtAction(...) | ResponseEntity.created(uri).body(...) |
| NotFound() | ResponseEntity.notFound().build() |
