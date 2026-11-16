---
from: csharp
status: draft
concept: "The API as a contract"
from_way: "Returning EF Core entities from controllers, including navigation properties and internal columns."
native_way: "Response records (`record OrderResponse(...)`) mapped from entities, with the OpenAPI document generated and reviewed."
sensei_tip: "ASP.NET Core 9+ generates OpenAPI with `AddOpenApi()`; diff the generated document in CI so a renamed property cannot slip out unnoticed."
---

| C# / ASP.NET Core | API concept |
| --- | --- |
| EF Core entity | implementation |
| `record` DTO | representation |
| `Microsoft.AspNetCore.OpenApi` | the contract document |
| `[JsonIgnore]` on entities | a missing DTO |
