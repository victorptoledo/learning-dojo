---
from: python
status: draft
concept: "Staying useful under overload"
from_way: "requests without a timeout, Celery with unlimited queues, and retries in a loop."
native_way: "Timeouts everywhere, bounded queues, retries with exponential backoff and jitter on idempotent calls only, and early rejection under load."
sensei_tip: "tenacity's wait_random_exponential implements full jitter; combine it with stop_after_attempt(3) and retry only on timeouts and 503s. asyncio.Semaphore caps concurrency to a dependency."
---

| Python | Overload concept |
| --- | --- |
| `requests.get(url, timeout=(3, 10))` | connect and read timeouts |
| `tenacity.retry(wait=wait_random_exponential(max=10))` | backoff with jitter |
| `queue.Queue(maxsize=1000)` + `put_nowait` | a bounded queue that pushes back |
| `asyncio.Semaphore(50)` | a concurrency limit |
| Celery `expires=` on a task | drop work past its deadline |
