---
from: csharp
status: draft
concept: "Webhooks and asynchronous APIs"
from_way: "Fire-and-forget `HttpClient.PostAsync` calls from controllers."
native_way: "An outbox plus a `BackgroundService` (or MassTransit) delivering signed events with retries; receivers verify with `CryptographicOperations.FixedTimeEquals`."
sensei_tip: "Read the raw body with `Request.EnableBuffering()` before model binding so you can compute the HMAC over the exact bytes received."
---

| C# / ASP.NET Core | Async concept |
| --- | --- |
| `BackgroundService` | delivery worker |
| `HMACSHA256` | signature |
| `CryptographicOperations.FixedTimeEquals` | constant-time verification |
| `Results.Accepted(uri)` | 202 for long jobs |
| MassTransit outbox | reliable events |
