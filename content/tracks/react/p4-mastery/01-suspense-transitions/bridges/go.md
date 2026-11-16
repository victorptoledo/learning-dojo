---
from: go
status: draft
concept: "Waiting and failing per region"
from_way: "A handler waits for goroutines with errgroup and returns one response; a failure in one call usually fails the whole request."
native_way: "React lets each region of the page wait (Suspense) and fail (error boundary) on its own, so the rest stays usable."
sensei_tip: "Think of each Suspense boundary as its own errgroup with a placeholder, and each error boundary as recovering from that group's failure without failing the page."
---

| Go | React |
| --- | --- |
| `errgroup.Wait()` | Suspense reveals when all children are ready |
| `singleflight` / a cache of results | Promise cache for `use` |
| `recover()` in a deferred func | Error boundary |
| Retry loop | Reset the boundary, evict the cached promise |
| Low-priority worker | Transition |
| Respond before the write finishes | `useOptimistic` |
