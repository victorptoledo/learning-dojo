---
from: javascript
status: draft
concept: "HTTP endpoints in Spring MVC"
from_way: "Express routes with handlers, or NestJS @Controller with @Get and DTO classes."
native_way: "@RestController with mapping annotations and records."
sensei_tip: "NestJS controllers and Spring controllers are nearly identical: @Get(':id') is @GetMapping(\"/{id}\"), @Param is @PathVariable, @Body is @RequestBody."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| app.get('/books/:id', handler) | @GetMapping("/books/{id}") |
| req.params.id | @PathVariable long id |
| req.body | @RequestBody BookRequest |
| res.status(201).json(...) | ResponseEntity.status(201).body(...) |
| req.query.page | @RequestParam int page |
