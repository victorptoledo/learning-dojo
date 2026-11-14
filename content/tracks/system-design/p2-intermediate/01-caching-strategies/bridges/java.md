---
from: java
status: draft
concept: "Cache invalidation and consistency"
from_way: "Adding a caching annotation without knowing what happens when the cache is unreachable."
native_way: "Choose the strategy explicitly: cache-aside for read-heavy data, write-through for critical consistency; add jitter to TTLs to avoid mass expiry."
sensei_tip: "Beware the cache stampede (thundering herd): when a hot key expires, thousands of requests hit the database at once. Use a lock or pre-warming."
---
