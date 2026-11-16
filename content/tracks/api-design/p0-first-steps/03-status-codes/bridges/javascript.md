---
from: javascript
status: draft
concept: "Choosing status codes"
from_way: "`res.json({ ok: false })` with status 200, or `throw` that Express turns into an HTML 500."
native_way: "`res.status(201).location(url).json(x)`, `res.sendStatus(204)`, and an error-handling middleware that maps errors to statuses."
sensei_tip: "`fetch` does not reject on 4xx or 5xx: check `response.ok` or `response.status` yourself."
---

| JavaScript / TypeScript | Status code |
| --- | --- |
| `res.status(201).location(url)` | 201 + Location |
| `res.sendStatus(204)` | 204 |
| `res.status(409)` | 409 |
| error middleware `(err, req, res, next)` | one place for status decisions |
| `response.ok` | 2xx check on the client |
