---
from: csharp
status: draft
concept: "Routing and caching at the edge"
from_way: "ASP.NET Core serving wwwroot with UseStaticFiles and output caching in the app."
native_way: "The CDN in front serves wwwroot and cacheable pages near users; the app sets Cache-Control and handles what must be dynamic."
sensei_tip: "asp-append-version on script and link tags adds a content hash as a query string; make sure the CDN keeps the query string in its cache key, or use fingerprinted file names instead."
---

| C# / ASP.NET Core | Edge concept |
| --- | --- |
| `[ResponseCache(Duration = 60, Location = ResponseCacheLocation.Any)]` | `Cache-Control: public, max-age=60` |
| `asp-append-version="true"` | cache busting by content hash |
| `Dns.GetHostAddressesAsync(host)` | a DNS lookup |
| Output caching middleware | an app cache, behind the CDN |
| Azure Front Door / Traffic Manager | anycast edge / DNS-based routing |
