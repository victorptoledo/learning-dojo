---
from: java
status: draft
concept: "Resources and REST"
from_way: "Endpoints like `@PostMapping(\"/getUser\")` or `/deleteProduct`, mirroring service methods."
native_way: "A controller per resource: `@RequestMapping(\"/orders\")` with `@GetMapping(\"/{id}\")`, `@PostMapping`, `@PatchMapping(\"/{id}\")`; actions as sub-resources such as `@PostMapping(\"/{id}/cancellation\")`."
sensei_tip: "Spring HATEOAS adds level-3 links if you need them; most APIs only need `ResponseEntity.created(location)` and a `Link` header for pages."
---

| Java / Spring | REST concept |
| --- | --- |
| `@RequestMapping("/orders")` | collection |
| `@GetMapping("/{id}")` | item |
| `@PostMapping("/{id}/cancellation")` | action as a resource |
| `@PatchMapping` + merge patch | partial update |
| Spring HATEOAS `EntityModel` | hypermedia (level 3) |
