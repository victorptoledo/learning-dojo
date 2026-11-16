---
from: java
status: draft
concept: "Idempotency keys and safe retries"
from_way: "Spring Retry or Resilience4j wrapped around a `RestClient.post()` without any key: retries create duplicates."
native_way: "Generate a UUID per operation, send it as `Idempotency-Key` on every attempt, retry with Resilience4j backoff and jitter only on transient failures; the server records keys with a unique constraint."
sensei_tip: "Resilience4j's `IntervalFunction.ofExponentialRandomBackoff` gives backoff with jitter; restrict `retryExceptions` to timeouts and 5xx so a 422 is never retried."
---

| Java / Spring | Idempotency concept |
| --- | --- |
| `UUID.randomUUID()` per operation | idempotency key |
| `RestClient...header("Idempotency-Key", key)` | sending it |
| `IntervalFunction.ofExponentialRandomBackoff` | backoff with jitter |
| unique constraint on `(client_id, key)` | atomic new-key check |
| `@Transactional` around effect + stored response | no gap between them |
