---
from: go
status: draft
concept: "Staying useful under overload"
from_way: "A goroutine per request without limits and an http.Client without a timeout."
native_way: "context deadlines through every call, bounded channels and semaphores, retries with jitter, and shedding when saturated."
sensei_tip: "A buffered channel used with select { case ch <- job: default: reject() } is a bounded queue with load shedding in four lines; context.WithTimeout carries the deadline to every downstream call."
---

| Go | Overload concept |
| --- | --- |
| `context.WithTimeout(ctx, 300*time.Millisecond)` | a deadline that propagates |
| `select` with `default` on a buffered channel | reject when the queue is full |
| `golang.org/x/sync/semaphore` | a concurrency limit |
| `http.Client{Timeout: 2 * time.Second}` | a request timeout |
| `rand.Int64N(backoff)` before retrying | full jitter |
