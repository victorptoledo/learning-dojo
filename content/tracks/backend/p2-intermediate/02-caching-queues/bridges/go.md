---
from: go
status: draft
concept: "Non-blocking operations"
from_way: "sync.Map caches, groupcache and x/sync/singleflight, plus goroutines for background work."
native_way: "Caching patterns and durable queues."
sensei_tip: "golang.org/x/sync/singleflight is exactly the stampede protection in this lesson: concurrent callers for one key share one load."
---

| Go | In practice |
| --- | --- |
| map with a mutex | in-process cache |
| singleflight.Group | stampede protection |
| go-redis | a shared cache |
| goroutine worker pool | in-process queue; use a broker for durability |
