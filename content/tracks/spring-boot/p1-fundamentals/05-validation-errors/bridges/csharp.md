---
from: csharp
status: draft
concept: "Input checks and error responses"
from_way: "DataAnnotations with [ApiController] automatic 400 responses and ProblemDetails."
native_way: "Bean Validation and ProblemDetail, very close to ASP.NET Core's model."
sensei_tip: "ASP.NET Core and Spring both speak RFC 9457 Problem Details. [Required] is @NotNull or @NotBlank, and IExceptionHandler is an @ExceptionHandler method."
---

| C# | Spring Boot 4 |
| --- | --- |
| [Required] | @NotBlank / @NotNull |
| [EmailAddress] | @Email |
| ValidationProblemDetails | ProblemDetail with field errors |
| IExceptionHandler | @ExceptionHandler |
