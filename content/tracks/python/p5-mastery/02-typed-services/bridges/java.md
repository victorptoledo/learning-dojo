---
from: java
status: draft
concept: "Spring MVC concepts in FastAPI"
from_way: "@RestController with @GetMapping and @PostMapping, @RequestBody with @Valid DTOs, constructor injection of beans, @ControllerAdvice for errors, application.yml with @ConfigurationProperties, and MockMvc in tests."
native_way: "Path operation functions with @app.get and @app.post, Pydantic models as parameters, Depends for per-request dependencies, exception handlers, pydantic-settings, and TestClient with dependency_overrides."
sensei_tip: "There is no container of singletons: Depends builds dependencies per request, and long-lived objects (an HTTP client pool) live in the lifespan and app.state."
---

| Spring Boot | FastAPI |
| --- | --- |
| `@GetMapping("/orders/{id}")` | `@app.get("/orders/{order_id}")` |
| `@RequestBody @Valid OrderIn in` | `order: OrderIn` (validated by Pydantic) |
| `@RequestParam boolean lines` | `include_lines: bool = False` |
| constructor injection of beans | `Annotated[Session, Depends(get_session)]` |
| `@ControllerAdvice` + `@ExceptionHandler` | `@app.exception_handler(PaymentError)` |
| `ResponseStatusException(NOT_FOUND)` | `HTTPException(status_code=404)` |
| `@ConfigurationProperties` | `pydantic_settings.BaseSettings` |
| MockMvc + `@MockitoBean` | `TestClient` + `app.dependency_overrides` |
