---
from: csharp
status: draft
concept: "Evolving an API without breaking clients"
from_way: "Renaming a property in the DTO and regenerating the client."
native_way: "Additive properties, `[Obsolete]` during the overlap, `Asp.Versioning.Http` for URL or header versions, and contract tests."
sensei_tip: "`Asp.Versioning` can report supported and deprecated versions in `api-supported-versions` and `api-deprecated-versions` headers automatically."
---

| C# / ASP.NET Core | Evolution concept |
| --- | --- |
| new nullable property | compatible change |
| `[Obsolete]` + OpenAPI `deprecated` | expand, then contract |
| `Asp.Versioning.Http` | versioning scheme |
| `JsonSerializerOptions.UnmappedMemberHandling = Skip` | tolerant reader |
| PactNet | consumer contract tests |
