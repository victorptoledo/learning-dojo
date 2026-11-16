---
from: csharp
status: draft
concept: "Governance, style guides and review"
from_way: "Roslyn analyzers for code style, no rules for the HTTP surface."
native_way: "Spectral on the build-time OpenAPI document, shared NuGet packages for problem details and pagination, and a reviewed document diff per change."
sensei_tip: "A shared NuGet package with `AddCompanyProblemDetails()` and a `CursorPage<T>` type turns half of the style guide into a single line of setup."
---

| C# / ASP.NET Core | Governance concept |
| --- | --- |
| Spectral on the generated document | automated style guide |
| shared NuGet package | consistency by reuse |
| Roslyn analyzers | code conventions |
| document diff in the pull request | design review |
| API portal / Backstage | API catalogue |
