---
from: java
status: draft
concept: "Spring MVC controllers and servlet filters"
from_way: "@RestController classes with @GetMapping and @PathVariable, Jackson for JSON, servlet filters and interceptors, an embedded Tomcat configured in application.yml, and RestClient or WebClient for outgoing calls."
native_way: "Plain handler functions registered on a ServeMux with method and wildcard patterns, encoding/json, middleware as func(http.Handler) http.Handler, an http.Server configured in code, and http.Client."
sensei_tip: "There is no container and no annotation scanning: main builds the dependencies, the mux and the server in about thirty lines you can read top to bottom."
---

| Spring | Go net/http |
| --- | --- |
| `@GetMapping("/todos/{id}")` | `mux.HandleFunc("GET /todos/{id}", h)` |
| `@PathVariable long id` | `strconv.Atoi(r.PathValue("id"))` |
| `@RequestBody CreateTodo in` | `json.NewDecoder(r.Body).Decode(&in)` |
| `ResponseEntity.status(201).body(t)` | `w.WriteHeader(201)` + `json.NewEncoder(w).Encode(t)` |
| `OncePerRequestFilter` | `func(next http.Handler) http.Handler` |
| `@ControllerAdvice` error mapping | a `writeError` helper or a handler type that returns `error` |
| `server.tomcat.*` timeouts | `http.Server{ReadTimeout: ..., WriteTimeout: ...}` |
| graceful shutdown property | `signal.NotifyContext` + `srv.Shutdown(ctx)` |
| `MockMvc` | `httptest.NewRecorder()` |
