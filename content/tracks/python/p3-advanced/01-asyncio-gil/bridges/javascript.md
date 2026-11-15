---
from: javascript
status: draft
concept: "One event loop, explicitly started"
from_way: "An event loop that always runs, promises created eagerly, async and await, Promise.all and Promise.allSettled, AbortController for cancellation, and worker threads for CPU work."
native_way: "An event loop started by asyncio.run, coroutines that do nothing until awaited or scheduled as tasks, TaskGroup and gather, asyncio.timeout and task cancellation, and processes for CPU work."
sensei_tip: "The model is Node's, with two differences: a Python coroutine does not start until you await it or create a task, and cancellation is built in (a CancelledError raised at the await), so you rarely need an AbortController."
---

| JavaScript (Node) | Python (asyncio) |
| --- | --- |
| implicit loop | `asyncio.run(main())` |
| `fetch(url)` starts at once | `fetch(url)` creates a coroutine; `await` or `create_task` runs it |
| `Promise.all(ps)` | `TaskGroup` or `asyncio.gather(*coros)` |
| `Promise.allSettled(ps)` | `asyncio.gather(*coros, return_exceptions=True)` |
| `AbortSignal.timeout(3000)` | `async with asyncio.timeout(3):` |
| `controller.abort()` | `task.cancel()` |
| `p-limit(50)` | `asyncio.Semaphore(50)` |
| `worker_threads` | `ProcessPoolExecutor` |
