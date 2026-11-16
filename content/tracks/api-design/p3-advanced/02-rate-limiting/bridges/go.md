---
from: go
status: draft
concept: "Rate limiting"
from_way: "A global `time.Ticker` that every handler waits on."
native_way: "`golang.org/x/time/rate` token buckets per key (in memory for one instance) or Redis-based limits for a fleet; middleware that answers 429 with `Retry-After`."
sensei_tip: "`rate.Limiter.Reserve().Delay()` tells you how long a client would have to wait: a ready-made `Retry-After` value."
---

| Go | Rate-limit concept |
| --- | --- |
| `rate.NewLimiter(rate.Limit(10), 50)` | token bucket (10/s, burst 50) |
| `limiter.Allow()` | decision |
| `Reserve().Delay()` | Retry-After |
| `map[string]*rate.Limiter` + mutex | per-key limits on one instance |
| Redis script | shared counters |
