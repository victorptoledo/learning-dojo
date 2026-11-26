---
from: csharp
status: draft
concept: "ASP.NET Core minimal APIs in FastAPI"
from_way: "Minimal APIs with app.MapGet and typed parameters, model binding and validation, the built-in DI container with scoped services, IOptions<T> for configuration, and WebApplicationFactory in tests."
native_way: "Path operations with @app.get and typed parameters, Pydantic validation, Depends for per-request (scoped) dependencies, pydantic-settings, and TestClient with dependency_overrides."
sensei_tip: "ASP.NET Core minimal APIs and FastAPI are remarkably alike. Scoped services map to Depends (one per request), singletons to objects created in lifespan."
---

| ASP.NET Core | FastAPI |
| --- | --- |
| `app.MapGet("/orders/{id:int}", ...)` | `@app.get("/orders/{order_id}")` with `order_id: int` |
| `[FromBody] OrderIn order` | `order: OrderIn` |
| `Results.NotFound()` | `raise HTTPException(status_code=404)` |
| scoped service (`AddScoped`) | a dependency with `Depends` |
| singleton (`AddSingleton`) | an object created in `lifespan` |
| `IOptions<OrderSettings>` | `Settings(BaseSettings)` |
| Swashbuckle / built-in OpenAPI | `/docs` and `/openapi.json` |
| `WebApplicationFactory<Program>` | `TestClient(app)` + `dependency_overrides` |
