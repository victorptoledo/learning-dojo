---
from: csharp
status: draft
concept: "Choosing status codes"
from_way: "`return Ok(new { error = \"...\" })` from every action."
native_way: "`Results.Created(uri, x)`, `Results.NoContent()`, `Results.NotFound()`, `Results.Conflict()`, `TypedResults` for compile-time checked responses."
sensei_tip: "Use `TypedResults` with a `Results<Ok<T>, NotFound>` return type: the possible statuses become part of the signature and the OpenAPI document."
---

| C# / ASP.NET Core | Status code |
| --- | --- |
| `TypedResults.Created(uri, x)` | 201 + Location |
| `TypedResults.NoContent()` | 204 |
| `TypedResults.NotFound()` | 404 |
| `StatusCodes.Status422UnprocessableEntity` | 422 |
| `Results<Ok<T>, NotFound>` | statuses in the signature |
