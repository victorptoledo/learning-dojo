---
from: javascript
status: draft
concept: "Idempotency keys and safe retries"
from_way: "Retrying `fetch` in a loop with a new body each time."
native_way: "`crypto.randomUUID()` once per operation, `p-retry` (or a small loop) with exponential backoff and jitter; on the server, Redis `SET key value NX EX 86400` or a unique index."
sensei_tip: "`AbortSignal.timeout(ms)` bounds each attempt; combine it with a total deadline so retries cannot run forever."
---

| JavaScript / TypeScript | Idempotency concept |
| --- | --- |
| `crypto.randomUUID()` | idempotency key |
| `fetch(url, { headers: { 'Idempotency-Key': key } })` | sending it |
| `p-retry` | retry policy |
| Redis `SET ... NX` | atomic new-key check |
| `AbortSignal.timeout` | per-attempt timeout |
