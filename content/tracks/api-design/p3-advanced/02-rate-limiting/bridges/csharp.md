---
from: csharp
status: draft
concept: "Rate limiting"
from_way: "A static counter in a controller."
native_way: "`Microsoft.AspNetCore.RateLimiting` (`AddRateLimiter` with token bucket, fixed, sliding window or concurrency limiters), partitioned by API key; a distributed store or gateway for the fleet."
sensei_tip: "`RateLimitPartition.GetTokenBucketLimiter(apiKey, ...)` partitions per client; set `OnRejected` to add `Retry-After` and a problem body."
---

| C# / ASP.NET Core | Rate-limit concept |
| --- | --- |
| `AddTokenBucketLimiter` | token bucket |
| `AddConcurrencyLimiter` | concurrency limit |
| `RateLimitPartition` by API key | limit key |
| `options.OnRejected` | 429 response |
| `.RequireRateLimiting("policy")` | per endpoint |
