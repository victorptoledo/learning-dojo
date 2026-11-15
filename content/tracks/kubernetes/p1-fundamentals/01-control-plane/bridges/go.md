---
from: go
status: draft
concept: "The declarative reconciliation loop"
from_way: "A goroutine with a ticker that checks state and fixes it, written ad hoc for each service."
native_way: "client-go gives every controller the same parts: informers that watch and cache, a work queue, and a reconcile function that is safe to run any number of times."
sensei_tip: "Read the sample-controller repository: it is the shortest complete example of the pattern every built-in controller follows."
---

| Go world | Kubernetes |
| --- | --- |
| `for range time.Tick(...)` | a resync period |
| a channel of events | an informer's event handlers |
| a worker pool reading a channel | a rate-limited work queue |
| an idempotent fix function | `Reconcile(ctx, key)` |
