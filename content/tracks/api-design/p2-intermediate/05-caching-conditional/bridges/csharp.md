---
from: csharp
status: draft
concept: "HTTP caching and conditional requests"
from_way: "`IMemoryCache` in controllers, no HTTP validators."
native_way: "`ETag` and `Cache-Control` headers set explicitly (or output caching), `Request.GetTypedHeaders().IfNoneMatch`, and `[ConcurrencyCheck]`/row versions for `If-Match`."
sensei_tip: "EF Core's `rowversion` (`[Timestamp]`) is a ready-made strong ETag source and gives you optimistic concurrency on `SaveChanges`."
---

| C# / ASP.NET Core | HTTP caching |
| --- | --- |
| `Response.Headers.ETag = "\"7\""` | ETag |
| `Request.GetTypedHeaders().IfNoneMatch` | conditional read |
| `StatusCodes.Status304NotModified` | 304 |
| `[Timestamp] byte[] RowVersion` | ETag source and optimistic locking |
| `Status412PreconditionFailed` | lost update prevented |
