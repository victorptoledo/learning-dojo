---
from: javascript
status: draft
concept: "Evolving an API without breaking clients"
from_way: "TypeScript clients that `switch` exhaustively over a status union and throw in `default`."
native_way: "Additive changes, unions with a fallback branch, a `/v2` router when needed, and an OpenAPI diff (oasdiff) in CI."
sensei_tip: "zod's `.passthrough()` or default stripping both tolerate new fields; but `z.enum([...])` rejects new values, so use `.catch('UNKNOWN')` on clients."
---

| JavaScript / TypeScript | Evolution concept |
| --- | --- |
| optional new property | compatible change |
| `z.enum([...]).catch('UNKNOWN')` | open enum on the client |
| `app.use('/v2', v2Router)` | path version |
| `res.set('Sunset', ...)` | retirement notice |
| oasdiff in CI | breaking-change check |
