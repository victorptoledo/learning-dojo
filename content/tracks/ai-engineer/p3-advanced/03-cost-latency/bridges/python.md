---
from: python
status: draft
concept: "Pay per token, wait per token"
from_way: "Optimising CPU time and database queries, with functools.lru_cache for repeated work."
native_way: "Budgeting tokens, caching prompt prefixes and answers, routing to cheaper models, running calls concurrently with asyncio and retrying with backoff and jitter."
sensei_tip: "The official SDKs retry a few times by default; set max_retries and timeout explicitly, and use asyncio.Semaphore plus a token bucket to stay under your tokens-per-minute limit."
---

| Python | Cost and latency |
| --- | --- |
| `functools.lru_cache` | answer caching (with a version in the key) |
| `tenacity` with `wait_random_exponential` | backoff with jitter |
| `asyncio.gather` / `TaskGroup` | parallel calls |
| `asyncio.Semaphore` | concurrency limit |
| `time.monotonic()` | measuring latency |
