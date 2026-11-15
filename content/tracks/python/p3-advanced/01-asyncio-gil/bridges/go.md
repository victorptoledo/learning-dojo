---
from: go
status: draft
concept: "Goroutines versus coroutines"
from_way: "Goroutines scheduled across every core, blocking calls that never block other goroutines, sync.WaitGroup and errgroup, context.Context for timeouts and cancellation, and buffered channels as semaphores."
native_way: "Coroutines on one thread with explicit await points, TaskGroup instead of errgroup, asyncio.timeout and task cancellation instead of a context, and asyncio.Semaphore and Queue."
sensei_tip: "Goroutines make any blocking call safe; in asyncio a blocking call stalls the whole loop. errgroup.WithContext maps closely to TaskGroup: the first error cancels the rest."
---

| Go | Python |
| --- | --- |
| `go fetch(url)` | `tg.create_task(fetch(url))` |
| `errgroup.WithContext(ctx)` + `g.Wait()` | `async with asyncio.TaskGroup() as tg:` |
| `context.WithTimeout(ctx, 3*time.Second)` | `async with asyncio.timeout(3):` |
| `ctx.Done()` | `CancelledError` raised at the await |
| `sem := make(chan struct{}, 50)` | `asyncio.Semaphore(50)` |
| channels between goroutines | `asyncio.Queue` |
| goroutines using every core | processes, or the free-threaded build with threads |
| `sync.Mutex` | `threading.Lock` / `asyncio.Lock` |
