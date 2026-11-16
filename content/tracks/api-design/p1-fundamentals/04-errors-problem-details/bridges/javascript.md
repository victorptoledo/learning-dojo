---
from: javascript
status: draft
concept: "Problem details (RFC 9457)"
from_way: "`res.status(400).json({ error: 'Bad request' })` with a different shape per route."
native_way: "One error middleware that writes `application/problem+json` objects; zod issues mapped to `errors: [{ pointer, detail }]`."
sensei_tip: "zod's `issue.path` (`['lines', 1, 'sku']`) maps directly to a JSON pointer: `'#/' + path.join('/')`."
---

| JavaScript / TypeScript | Problem details |
| --- | --- |
| `res.type('application/problem+json')` | problem media type |
| `class ApiProblem extends Error` | typed problem |
| `zodError.issues` | validation errors |
| error middleware | one mapping point |
| `crypto.randomUUID()` in logs and `instance` | correlation id |
