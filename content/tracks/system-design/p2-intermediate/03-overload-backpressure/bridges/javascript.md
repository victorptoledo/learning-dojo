---
from: javascript
status: draft
concept: "Staying useful under overload"
from_way: "Promise.all over thousands of fetches, and an event loop that keeps accepting connections until memory runs out."
native_way: "Cap concurrency, time out every call, back off with jitter, and reject early when the server is saturated."
sensei_tip: "Node accepts connections faster than it can serve them; watch event-loop lag (perf_hooks.monitorEventLoopDelay) and return 503 when it climbs. p-limit caps concurrency for outbound calls."
---

| JavaScript | Overload concept |
| --- | --- |
| `fetch(url, { signal: AbortSignal.timeout(2000) })` | a request timeout |
| `p-limit(10)` | a concurrency limit |
| `p-retry` with `randomize: true` | backoff with jitter |
| event-loop delay monitoring | detecting saturation |
| `res.status(503).set('Retry-After', '5')` | cheap rejection |
