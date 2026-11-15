---
from: go
status: draft
concept: "Keeping a second copy close"
from_way: "A map guarded by a sync.RWMutex, or go-redis in front of a database."
native_way: "Bounded caches with expiry, delete on write, and coalesced loads; a map without eviction only grows."
sensei_tip: "golang.org/x/sync/singleflight is the canonical single-flight implementation: Do(key, fn) runs fn once while concurrent callers for the same key wait for its result."
---

| Go | Caching concept |
| --- | --- |
| `map` + `sync.RWMutex` | an unbounded cache: add eviction |
| `container/list` + `map` | the classic LRU |
| `singleflight.Group.Do` | request coalescing |
| `rdb.Set(ctx, k, v, 5*time.Minute)` | TTL in Redis |
| `hashicorp/golang-lru/v2/expirable` | LRU with TTL |
