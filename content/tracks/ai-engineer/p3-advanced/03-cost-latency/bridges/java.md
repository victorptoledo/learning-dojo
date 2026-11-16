---
from: java
status: draft
concept: "Pay per token, wait per token"
from_way: "Costs per server and per request are small and fixed; latency tuning means database indexes and connection pools."
native_way: "Every call has a variable price and latency proportional to tokens, so you budget tokens, cache prefixes, route to cheaper models and retry with backoff."
sensei_tip: "Resilience4j already gives you retry with exponential random backoff, a rate limiter, a bulkhead and a circuit breaker; configure them per model endpoint and log tokens with Micrometer."
---

| Java | Cost and latency |
| --- | --- |
| Caffeine or Redis cache | answer caching by request hash |
| Resilience4j `Retry` with random backoff | backoff with jitter |
| Resilience4j `RateLimiter` | client-side token bucket |
| `CircuitBreaker` | stop calling a failing provider |
| `CompletableFuture.allOf` | parallel independent calls |
