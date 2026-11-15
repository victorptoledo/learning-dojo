---
from: csharp
status: draft
concept: "From RPC endpoints to resources"
from_way: "ASP.NET Core controllers mapping actions, with Swashbuckle or the built-in OpenAPI."
native_way: "Resource design that the controllers implement."
sensei_tip: "Microsoft's REST guidelines and Problem Details support in ASP.NET Core line up with this lesson; apply them consistently across every controller."
---

| C# | In practice |
| --- | --- |
| [HttpPost("CreateOrder")] | [HttpPost] on /orders |
| JsonPatchDocument | merge patch for simple cases |
| ProblemDetails | application/problem+json |
| API versioning package | evolve additively first |
