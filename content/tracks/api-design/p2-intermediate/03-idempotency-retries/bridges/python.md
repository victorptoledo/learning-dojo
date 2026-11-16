---
from: python
status: draft
concept: "Idempotency keys and safe retries"
from_way: "`for _ in range(3): requests.post(...)` inside a `try`."
native_way: "`tenacity` with `wait_random_exponential` and `retry_if_exception_type`, a key from `uuid.uuid4()` reused across attempts; on the server, `INSERT ... ON CONFLICT DO NOTHING` decides new keys."
sensei_tip: "`tenacity.wait_random_exponential(multiplier=0.5, max=10)` is \"full jitter\" backoff in one line."
---

| Python | Idempotency concept |
| --- | --- |
| `uuid.uuid4()` | idempotency key |
| `tenacity.retry(...)` | retry policy |
| `wait_random_exponential` | backoff with jitter |
| `INSERT ... ON CONFLICT DO NOTHING` | atomic new-key check |
| `httpx` timeouts | bounded attempts |
