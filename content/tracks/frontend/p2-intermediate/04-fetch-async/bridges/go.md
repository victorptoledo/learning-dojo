---
from: go
status: draft
concept: "Async I/O"
from_way: "Goroutines, channels and context.Context for cancellation and deadlines."
native_way: "One thread, an event loop and Promises; AbortSignal plays the role of context.Context."
sensei_tip: "context.WithTimeout is AbortSignal.timeout, and ctx.Done() is the signal's abort event: the same cancellation discipline, just single-threaded."
---

| Go | In the browser |
| --- | --- |
| `http.NewRequestWithContext(ctx, ...)` | `fetch(url, { signal })` |
| `context.WithTimeout(ctx, 5*time.Second)` | `AbortSignal.timeout(5000)` |
| `errgroup.Wait()` | `Promise.all([...])` |
| `resp.StatusCode >= 400` | `!response.ok` |
