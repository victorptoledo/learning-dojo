---
from: go
status: draft
concept: "Webhooks and asynchronous APIs"
from_way: "`go http.Post(url, ...)` inside the handler, losing events on restart."
native_way: "A persistent queue or outbox drained by a worker pool, `crypto/hmac` signatures, exponential backoff with jitter, and `202` plus a status resource for long jobs."
sensei_tip: "`hmac.Equal` compares MACs in constant time; read the body once with `io.ReadAll` and verify before decoding JSON."
---

| Go | Async concept |
| --- | --- |
| worker goroutines reading an outbox | delivery |
| `hmac.New(sha256.New, secret)` | signature |
| `hmac.Equal` | constant-time verification |
| `http.StatusAccepted` | 202 for long jobs |
| event id with `ON CONFLICT DO NOTHING` | deduplication |
