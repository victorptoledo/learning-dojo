---
from: csharp
status: draft
concept: "Problem details (RFC 9457)"
from_way: "Ad-hoc `BadRequest(new { message })` objects."
native_way: "`builder.Services.AddProblemDetails()`, `Results.Problem(...)`, `Results.ValidationProblem(errors)`, `IExceptionHandler` for unexpected exceptions."
sensei_tip: "`AddProblemDetails()` plus `UseExceptionHandler()` and `UseStatusCodePages()` make even unhandled exceptions and bare 404s come out as problem details."
---

| C# / ASP.NET Core | Problem details |
| --- | --- |
| `Results.Problem(type: ..., title: ..., statusCode: ...)` | a problem |
| `ProblemDetails.Extensions["balance"]` | extension member |
| `Results.ValidationProblem(...)` | validation errors |
| `IExceptionHandler` | safe 500s |
| `AddProblemDetails(o => o.CustomizeProblemDetails = ...)` | instance and trace id |
