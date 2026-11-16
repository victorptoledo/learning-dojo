---
from: csharp
status: draft
concept: "HTTP methods, safety and idempotency"
from_way: "`[HttpPost]` actions named `GetCustomer`, `DeleteCustomer` on one controller."
native_way: "`[HttpGet]`, `[HttpPut]`, `[HttpPatch]`, `[HttpDelete]` or `MapGet`/`MapPut` with resource routes; `[Produces]` and `[Consumes]` for media types."
sensei_tip: "`HttpClient` with Polly or `Microsoft.Extensions.Http.Resilience` retries transient failures; configure it to skip non-idempotent methods unless they carry an idempotency key."
---

| C# / ASP.NET Core | HTTP concept |
| --- | --- |
| `MapGet` | safe |
| `MapPut` / `MapDelete` | idempotent |
| `MapPost` | not idempotent |
| `[Consumes("application/json")]` | 415 when it does not match |
| `Results.StatusCode(405)` | method not allowed |
