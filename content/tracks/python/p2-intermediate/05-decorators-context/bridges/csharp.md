---
from: csharp
status: draft
concept: "Attributes, middleware and IDisposable"
from_way: "Attributes as metadata read by frameworks, action filters and middleware for cross-cutting concerns, Polly policies for retries, and using statements over IDisposable."
native_way: "Decorators that wrap the function itself, retry and cache decorators, and with statements over context managers with __enter__ and __exit__."
sensei_tip: "C# attributes do nothing on their own; Python decorators run at definition time and replace the function. That is why @app.get(...) can register a route the moment the module is imported."
---

| C# | Python |
| --- | --- |
| `[Authorize]` attribute plus a filter | an `@requires_login` decorator |
| Polly `Policy.Handle<HttpRequestException>().Retry(3)` | `@retry(times=3, on=HTTPError)` |
| `IMemoryCache` around a method | `@lru_cache(maxsize=...)` |
| `using var conn = new SqlConnection(...)` | `with connect(...) as conn:` |
| `IDisposable.Dispose()` | `__exit__` |
| `IAsyncDisposable` / `await using` | `__aexit__` / `async with` |
| `TransactionScope` | a `transaction()` context manager |
