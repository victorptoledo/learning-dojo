---
from: csharp
status: draft
concept: "Resources and REST"
from_way: "`[Route(\"api/[action]\")]` with action names like `GetOrder` and `CancelOrder`."
native_way: "`MapGroup(\"/orders\")` with `MapGet(\"/{id}\")`, `MapPost(\"/\")`, `MapPost(\"/{id}/cancellation\")`; or attribute routes per resource."
sensei_tip: "Route groups (`app.MapGroup(\"/orders\")`) share filters, auth and OpenAPI tags per resource; they make resource boundaries visible in code."
---

| C# / ASP.NET Core | REST concept |
| --- | --- |
| `MapGroup("/orders")` | collection |
| `MapGet("/{id}")` | item |
| `MapPost("/{id}/cancellation")` | action as a resource |
| `JsonPatchDocument` / merge patch | partial update |
| `Results.Created(uri, x)` | link to the created resource |
