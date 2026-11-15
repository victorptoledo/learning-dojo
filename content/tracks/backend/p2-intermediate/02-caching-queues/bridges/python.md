---
from: python
status: draft
concept: "Non-blocking operations"
from_way: "functools.lru_cache, Django's cache framework and Celery for background work."
native_way: "Cache-aside with TTLs, invalidation rules, stampede protection and queues for slow work."
sensei_tip: "Django's cache.get_or_set is cache-aside; add jitter to the timeout and a lock for hot keys so a single expiry does not send every request to the database."
---

| Python | In practice |
| --- | --- |
| @lru_cache | an in-process cache |
| cache.get_or_set(key, fn, timeout) | cache-aside with a TTL |
| Celery task | a queue and a worker |
| redis-py | a shared cache |
