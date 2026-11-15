---
from: javascript
status: draft
concept: "Keeping a second copy close"
from_way: "A Map in module scope, React Query's cache in the browser, or ioredis on the server."
native_way: "Each layer is a copy with its own lifetime; decide staleness per data, delete on write, and coalesce concurrent misses."
sensei_tip: "Storing the Promise instead of the value in a Map is single flight in JavaScript: concurrent callers await the same in-flight load. Remove it on rejection, or one failure is cached forever."
---

| JavaScript | Caching concept |
| --- | --- |
| `lru-cache` package with `ttl` | in-process LRU with expiry |
| `map.set(key, loadPromise)` | request coalescing (single flight) |
| React Query `staleTime` | stale-while-revalidate in the client |
| `redis.set(key, v, { EX: 300 })` | TTL in Redis |
| `Cache-Control: stale-while-revalidate=30` | the same idea at the CDN |
