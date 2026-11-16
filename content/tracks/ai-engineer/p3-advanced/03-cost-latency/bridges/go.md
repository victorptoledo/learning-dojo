---
from: go
status: draft
concept: "Pay per token, wait per token"
from_way: "Tuning goroutines, allocations and database pools; requests cost almost nothing."
native_way: "Token budgets, prefix caching, model routing, bounded concurrency and retries with backoff and jitter around every model call."
sensei_tip: "golang.org/x/time/rate is a token bucket; call limiter.WaitN(ctx, estimatedTokens) before each request to stay under tokens per minute, and use context deadlines for timeouts."
---

| Go | Cost and latency |
| --- | --- |
| an LRU map keyed by request hash | answer caching |
| `rate.Limiter.WaitN` | token bucket for tokens per minute |
| `errgroup` with `SetLimit` | bounded parallel calls |
| `time.Sleep(rand.N(cap))` | full-jitter backoff |
| `context.WithTimeout` | timeouts |
