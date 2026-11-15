---
from: python
status: draft
concept: "Keeping a second copy close"
from_way: "functools.lru_cache on a function, or Django's cache framework with a Redis backend."
native_way: "Choose per data the layer, the staleness budget, the invalidation rule and the stampede protection; an unbounded process cache is a memory leak with good intentions."
sensei_tip: "lru_cache has no TTL and is per process: with 8 Gunicorn workers you have 8 caches. Use cachetools.TTLCache for local expiry and Redis for a shared copy."
---

| Python | Caching concept |
| --- | --- |
| `@functools.lru_cache(maxsize=1024)` | in-process LRU, no TTL |
| `cachetools.TTLCache` | LRU with expiry |
| `cache.get_or_set(key, loader, timeout=300)` | cache-aside |
| `redis.set(key, val, nx=True, px=30000)` | a lock for single flight |
| `cache.delete(key)` after `save()` | delete on write |
