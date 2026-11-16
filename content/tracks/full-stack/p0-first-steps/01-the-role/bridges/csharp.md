---
from: csharp
status: reviewed
concept: "Beyond the ASP.NET Core pipeline"
from_way: "An ASP.NET Core developer knows the middleware pipeline well but stops at Kestrel's door."
native_way: "The pipeline is one hop: the browser, a CDN, a reverse proxy and the database are all part of the feature you own."
sensei_tip: "Use the browser's network tab together with ASP.NET Core's logging of EF Core commands to see a whole request at once."
---

| .NET habit | End to end |
| --- | --- |
| middleware is the edge | DNS, TLS, CDN and load balancer come first |
| `ILogger` messages | structured logs that carry the trace id |
| `dotnet ef database update` | migrations as a release step |
| Razor or Blazor pages | the browser's runtime, caching and security rules apply |
