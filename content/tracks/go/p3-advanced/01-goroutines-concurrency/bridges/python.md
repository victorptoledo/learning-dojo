---
from: python
status: draft
concept: "asyncio, threads and the GIL"
from_way: "asyncio with async def and await on one event loop, threads limited by the GIL for CPU work, multiprocessing for parallelism, and free-threaded builds still young."
native_way: "Goroutines are ordinary functions: they block like threads, scale like asyncio tasks, and run in parallel on every core, with no await and no GIL."
sensei_tip: "There are no coloured functions in Go. A function that does I/O looks like any other; the caller decides whether to run it in a goroutine."
---

| Python | Go |
| --- | --- |
| `asyncio.create_task(fetch(url))` | `go fetch(url)` |
| `await asyncio.gather(*tasks)` | `wg.Wait()` after `wg.Go(...)` |
| `async def` / `await` everywhere | plain functions; blocking is cheap |
| `threading.Thread(target=f).start()` | `go f()` |
| the GIL | no global lock: parallel on GOMAXPROCS cores |
| `multiprocessing` for CPU work | goroutines already run in parallel |
| `asyncio.TaskGroup` | `errgroup.Group` (golang.org/x/sync) |
| `task.cancel()` | cancel a `context.Context` |
