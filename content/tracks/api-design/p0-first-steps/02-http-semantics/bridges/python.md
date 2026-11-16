---
from: python
status: draft
concept: "HTTP methods, safety and idempotency"
from_way: "Flask or Django views that accept any method and branch on `request.method`."
native_way: "One handler per method (`@app.get`, `@app.put` in FastAPI), with the method chosen for its semantics."
sensei_tip: "`requests` and `httpx` do not retry by default; urllib3's `Retry` only retries idempotent methods unless you add POST to `allowed_methods`. Do not."
---

| Python | HTTP concept |
| --- | --- |
| `@app.get(...)` | safe, idempotent |
| `@app.put(...)` | idempotent |
| `@app.post(...)` | not idempotent |
| `Retry(allowed_methods=...)` | which methods a client may retry |
| `Response(media_type=...)` | Content-Type |
