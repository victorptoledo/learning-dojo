---
from: javascript
status: draft
concept: "Rate limiting"
from_way: "`express-rate-limit` with its default in-memory store on a 6-instance deployment."
native_way: "`express-rate-limit` or `@fastify/rate-limit` with a Redis store, keyed by API key, sending standard headers."
sensei_tip: "`express-rate-limit` can send the IETF `RateLimit` headers (`standardHeaders: 'draft-8'`); set `keyGenerator` to the API key, not the IP."
---

| JavaScript / TypeScript | Rate-limit concept |
| --- | --- |
| `rateLimit({ windowMs, limit })` | window limit |
| `rate-limit-redis` | shared counters |
| `standardHeaders: 'draft-8'` | RateLimit headers |
| `keyGenerator: req => req.apiKey` | limit key |
| `res.status(429).set('Retry-After', s)` | 429 |
