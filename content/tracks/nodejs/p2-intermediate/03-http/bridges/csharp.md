---
from: csharp
status: draft
concept: "Kestrel and HttpClient"
from_way: "Kestrel hosts ASP.NET Core; minimal APIs or controllers route and bind; middleware handles errors; HttpClient with a Timeout calls other services."
native_way: "node:http is the server; routing and binding are yours or a framework's; one catch handles errors; fetch with an AbortSignal calls other services."
sensei_tip: "Results.Problem(...) and UseExceptionHandler map to one central error handler. Unlike HttpClient (100 s default), fetch has no overall timeout at all."
---

| ASP.NET Core | Node.js |
| --- | --- |
| `app.MapGet("/users/{id}", h)` | Route on `req.method` and `URL.pathname` |
| `HttpContext.Request.Body` | `req` (readable stream) |
| `Results.Created(location, x)` | `res.writeHead(201, { location })` |
| `UseExceptionHandler` | `.catch` around the router |
| `HttpClient.Timeout` | `AbortSignal.timeout(ms)` |
| Kestrel `MaxRequestBodySize` | Your own body limit |
