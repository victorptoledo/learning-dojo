---
from: csharp
status: reviewed
concept: "Response caching and concurrency tokens in ASP.NET Core"
from_way: "[ResponseCache] on everything, or nothing at all, and DbUpdateConcurrencyException surfacing as a 500."
native_way: "Cache-Control set per kind of response, ETags from a row version answering 304, If-Match checked against the concurrency token and mapped to 412."
sensei_tip: "EF Core's [Timestamp] row version is a ready-made ETag: return it, require it in If-Match, and map DbUpdateConcurrencyException to 412."
---

| ASP.NET Core | Data fetching |
| --- | --- |
| `[ResponseCache(Location = ResponseCacheLocation.Client, NoStore = false)]` | browser caching |
| `[Timestamp] byte[] RowVersion` | the ETag source |
| `DbUpdateConcurrencyException` | 412 Precondition Failed |
| output caching | server-side caching with tags to evict |
