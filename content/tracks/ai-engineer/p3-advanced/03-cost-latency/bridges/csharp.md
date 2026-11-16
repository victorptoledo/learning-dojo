---
from: csharp
status: draft
concept: "Pay per token, wait per token"
from_way: "Response caching and EF Core query tuning; costs are per server."
native_way: "Token-based costs and latency managed with prefix caching, model routing, rate limiting and resilience pipelines around the chat client."
sensei_tip: "Microsoft.Extensions.Http.Resilience (Polly v8) gives retry with jitter, rate limiting and circuit breaking for HttpClient-based model clients; System.Threading.RateLimiting has a TokenBucketRateLimiter."
---

| C# | Cost and latency |
| --- | --- |
| `IMemoryCache`, `HybridCache` | answer caching |
| Polly retry with `UseJitter` | backoff with jitter |
| `TokenBucketRateLimiter` | client-side rate limit |
| Polly circuit breaker | stop calling a failing provider |
| `Task.WhenAll` | parallel calls |
