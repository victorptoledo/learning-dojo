---
from: javascript
status: draft
concept: "Express or NestJS concepts in FastAPI"
from_way: "Express or Fastify routes with handlers taking req and res, zod or class-validator for bodies, middleware for auth, NestJS providers for injection, and supertest for tests."
native_way: "Typed path operation functions whose parameters are parsed and validated, Pydantic models, Depends for auth and resources, and TestClient with dependency_overrides."
sensei_tip: "Fastify with JSON schemas and NestJS with DTOs are the closest relatives. FastAPI's twist is that the schema is the type hint itself, so there is nothing to keep in sync."
---

| Express / NestJS | FastAPI |
| --- | --- |
| `app.get("/orders/:id", (req, res) => ...)` | `@app.get("/orders/{order_id}")` |
| `req.params.id` (a string) | `order_id: int` (parsed and validated) |
| `schema.parse(req.body)` (zod) | `order: OrderIn` |
| `res.status(404).json(...)` | `raise HTTPException(status_code=404)` |
| auth middleware | `Depends(current_user)` |
| NestJS providers | dependencies with `Depends` |
| `supertest(app).post(...)` | `TestClient(app).post(...)` |
| `process.env` | `pydantic_settings.BaseSettings` |
