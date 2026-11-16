---
from: go
status: draft
concept: "Idempotency keys and safe retries"
from_way: "A `for` loop around `http.Post` with `time.Sleep(time.Second)`."
native_way: "A key from `crypto/rand` (or a UUID library) per operation, backoff with jitter (`math/rand/v2`), a `context.Context` deadline, and `INSERT ... ON CONFLICT` on the server."
sensei_tip: "Pass the same `context` with a deadline through all attempts: when the caller gives up, the retries stop too."
---

| Go | Idempotency concept |
| --- | --- |
| UUID per operation | idempotency key |
| `req.Header.Set("Idempotency-Key", key)` | sending it |
| `rand.N(backoff)` | jitter |
| `context.WithTimeout` | overall deadline |
| `ON CONFLICT DO NOTHING` | atomic new-key check |
