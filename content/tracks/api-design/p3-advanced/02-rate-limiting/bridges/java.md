---
from: java
status: draft
concept: "Rate limiting"
from_way: "A `ConcurrentHashMap<String, Integer>` counter per instance, reset by a scheduled task."
native_way: "Bucket4j token buckets (with a Redis or JCache backend for the fleet) or the gateway's limiter (Spring Cloud Gateway `RequestRateLimiter`), answering 429 with `Retry-After`."
sensei_tip: "Bucket4j's `tryConsumeAndReturnRemaining` gives you both the decision and the numbers for `RateLimit` headers in one call."
---

| Java / Spring | Rate-limit concept |
| --- | --- |
| Bucket4j `Bandwidth.builder().capacity(..).refillGreedy(..)` | token bucket |
| Bucket4j Redis proxy | shared counters |
| `ConsumptionProbe.getRemainingTokens()` | remaining budget |
| `HttpStatus.TOO_MANY_REQUESTS` + `Retry-After` | 429 |
| Spring Cloud Gateway `RequestRateLimiter` | limit at the edge |
