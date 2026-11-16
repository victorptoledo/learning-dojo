---
from: csharp
status: draft
concept: "Blazor render modes"
from_way: "Blazor has static server rendering, streaming rendering, interactive server and interactive WebAssembly render modes, chosen per component with @rendermode."
native_way: "React frameworks render Server Components on the server (streamed with Suspense) and Client Components in the browser after hydration; 'use client' plays the role of an interactive render mode."
sensei_tip: "If you know Blazor's render modes, RSC is familiar: static server rendering by default, interactivity opted in per component. Server functions resemble form posts to minimal API endpoints."
---

| Blazor | React frameworks |
| --- | --- |
| Static SSR | Server Components |
| `[StreamRendering]` | Streaming with Suspense |
| `@rendermode InteractiveWebAssembly` | `"use client"` |
| Form post to an endpoint | Server function or form action |
| `[Authorize]` | Auth check in the server function |
| Prerendering | SSR / SSG |
