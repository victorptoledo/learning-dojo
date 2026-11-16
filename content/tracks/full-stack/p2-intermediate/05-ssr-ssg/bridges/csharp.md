---
from: csharp
status: reviewed
concept: "Blazor's render modes as a map of the strategies"
from_way: "Blazor WebAssembly for everything, downloading the runtime before anything is visible."
native_way: "Blazor's render modes mirror the spectrum: static SSR, streaming rendering, interactive server and interactive WebAssembly, chosen per page or component."
sensei_tip: "Static SSR with [StreamRendering] plus a few interactive components is .NET's version of islands."
---

| ASP.NET Core / Blazor | Rendering strategy |
| --- | --- |
| static SSR | SSR without client JavaScript |
| `[StreamRendering]` | streaming SSR |
| `@rendermode InteractiveWebAssembly` | client rendering with hydration-like start-up |
| output caching on Razor Pages | ISR-like caching |
