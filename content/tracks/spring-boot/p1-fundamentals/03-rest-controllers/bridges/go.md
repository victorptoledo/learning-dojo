---
from: go
status: draft
concept: "HTTP endpoints in Spring MVC"
from_way: "http.HandleFunc with method patterns (Go 1.22) and encoding/json."
native_way: "Annotated controller methods with automatic JSON binding."
sensei_tip: "Go 1.22's mux.HandleFunc(\"GET /books/{id}\", h) reads like @GetMapping(\"/books/{id}\"). Spring does the json.Decode and json.Encode for you with Jackson."
---

| Go | Spring Boot 4 |
| --- | --- |
| mux.HandleFunc("GET /books/{id}", h) | @GetMapping("/books/{id}") |
| r.PathValue("id") | @PathVariable long id |
| json.NewDecoder(r.Body).Decode(&req) | @RequestBody BookRequest req |
| w.WriteHeader(201) | ResponseEntity.status(201) |
