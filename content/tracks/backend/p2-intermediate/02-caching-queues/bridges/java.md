---
from: java
status: draft
concept: "Non-blocking operations"
from_way: "Sending emails and building reports inside the same synchronous HTTP request, and a static HashMap as a cache with no expiry."
native_way: "The endpoint enqueues a job in milliseconds and answers 202 Accepted; workers do the slow part. Caches have a layer, a TTL, a size bound and an invalidation rule."
sensei_tip: "Make background jobs idempotent: if a message is delivered twice, the end result must be the same."
---

| Habit | Better |
| --- | --- |
| `static Map<Long, Product> CACHE` | Caffeine with `maximumSize` and `expireAfterWrite` |
| `cache.put(key, newValue)` after a write | `cache.invalidate(key)` |
| `mailer.send(...)` inside the request | enqueue + `202 Accepted` |
| One fixed TTL for every key | TTL with jitter for hot keys |
