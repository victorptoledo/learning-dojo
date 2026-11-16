---
from: python
status: draft
concept: "Rate limiting"
from_way: "A module-level dict of counters inside one worker process."
native_way: "`slowapi`/`limits` with a Redis storage, or the gateway; 429 with `Retry-After` and rate-limit headers from middleware."
sensei_tip: "Gunicorn or Uvicorn workers are separate processes: in-memory counters give each worker its own limit."
---

| Python | Rate-limit concept |
| --- | --- |
| `limits` / `slowapi` | rate-limit library |
| Redis storage | shared counters |
| `@limiter.limit("100/minute")` | per-endpoint limit |
| `Retry-After` header | when to retry |
| key function on the API key | limit key |
