---
from: csharp
status: reviewed
concept: "OpenAPI from ASP.NET Core to a typed client"
from_way: "C# DTOs on the server and a front end that copies their shapes by hand."
native_way: "ASP.NET Core's built-in OpenAPI document (or Swashbuckle) feeds a generated TypeScript client; data annotations or FluentValidation validate; ProblemDetails reports errors."
sensei_tip: "AddProblemDetails() plus ValidationProblemDetails gives you RFC 9457 errors with an errors dictionary for free."
---

| ASP.NET Core | Contract |
| --- | --- |
| `record ProductDto(...)` | an OpenAPI schema |
| `[Required]`, `[StringLength]` | server-side validation |
| `Results.ValidationProblem(errors)` | 400 problem details with field errors |
| `MapOpenApi()` | the source for generated clients |
