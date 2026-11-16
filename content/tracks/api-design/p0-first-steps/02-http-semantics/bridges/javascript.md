---
from: javascript
status: draft
concept: "HTTP methods, safety and idempotency"
from_way: "`app.post('/api', ...)` with an action field, or `fetch` with GET for everything including deletes."
native_way: "Express/Fastify routes per method; `fetch(url, { method: 'PUT' })`; `Accept` and `Content-Type` set explicitly."
sensei_tip: "Browsers send a CORS preflight (`OPTIONS`) for `PUT`, `PATCH`, `DELETE` and JSON bodies; answer it, or the real request never leaves the browser."
---

| JavaScript / TypeScript | HTTP concept |
| --- | --- |
| `app.get` | safe |
| `app.put` / `app.delete` | idempotent |
| `app.post` | not idempotent |
| `fetch(url, { method })` | the method is the verb |
| `res.type('application/json')` | Content-Type |
