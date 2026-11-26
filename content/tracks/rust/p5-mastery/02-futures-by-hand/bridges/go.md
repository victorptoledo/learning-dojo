---
from: go
status: draft
concept: "Goroutines versus futures"
from_way: "Goroutines are cheap threads with stacks; the runtime parks them on channel and I/O waits."
native_way: "Futures are stackless state machines; an executor polls them and a waker says when to poll again."
sensei_tip: "Go hides the scheduler entirely; in Rust you can see (and even write) it, which is why the async ecosystem has several runtimes."
---

| Go | Rust |
| --- | --- |
| `go f()` | spawn a future on an executor |
| `sync.WaitGroup` | `join_all` |
| `select` with `time.After` | `timeout` or `tokio::select!` |
| `ctx.Done()` cancellation | drop the future |
