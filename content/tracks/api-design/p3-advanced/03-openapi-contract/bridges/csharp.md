---
from: csharp
status: draft
concept: "OpenAPI as a contract"
from_way: "Swashbuckle output that no one reviews."
native_way: "`Microsoft.AspNetCore.OpenApi` (`AddOpenApi`) generating the document at build time, committed and diffed; NSwag or Kiota generating clients."
sensei_tip: "ASP.NET Core can emit the OpenAPI document at build time (`Microsoft.Extensions.ApiDescription.Server`), so CI can diff it without running the app."
---

| C# / ASP.NET Core | OpenAPI concept |
| --- | --- |
| `builder.Services.AddOpenApi()` | code-first document |
| build-time document generation | a file to diff |
| Kiota / NSwag | SDK generation |
| `[ProducesResponseType]` / `TypedResults` | documented responses |
| Spectral | style checks |
