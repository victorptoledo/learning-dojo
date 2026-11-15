---
from: csharp
status: draft
concept: "Staying useful under overload"
from_way: "HttpClient with the 100-second default timeout and Task.WhenAll over every request."
native_way: "Short timeouts, a resilience pipeline (retry with jitter, circuit breaker, concurrency limiter) and early rejection when the server is saturated."
sensei_tip: "ASP.NET Core has a built-in rate-limiting middleware with a ConcurrencyLimiter and a queue limit; Polly v8 (Microsoft.Extensions.Http.Resilience) adds retries with jitter, timeouts and circuit breakers to HttpClient."
---

| C# / .NET | Overload concept |
| --- | --- |
| `HttpClient.Timeout` / `CancellationToken` deadlines | timeouts and deadlines |
| Polly `RetryStrategyOptions { UseJitter = true }` | backoff with jitter |
| `AddConcurrencyLimiter(o => o.QueueLimit = 100)` | concurrency limit with a bounded queue |
| `Channel.CreateBounded<T>(1000)` | a bounded queue with back-pressure |
| `AddStandardResilienceHandler()` | a ready-made pipeline |
