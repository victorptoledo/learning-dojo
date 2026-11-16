---
from: python
status: draft
concept: "One event loop, always running"
from_way: "asyncio's loop runs ready callbacks and coroutines and waits in a selector; loop.call_soon schedules, run_in_executor offloads blocking work to threads."
native_way: "libuv's loop has fixed phases (timers, poll, check ...) plus the nextTick and microtask queues drained after each callback; setImmediate schedules, the thread pool runs fs and crypto."
sensei_tip: "A blocking call in an async def blocks asyncio exactly as a sync call blocks Node.js. The difference: in Node.js the standard library is async by default, so the blocking calls are the ones ending in Sync."
---

| Python asyncio | Node.js |
| --- | --- |
| `loop.call_soon(f)` | `setImmediate(f)` (or `queueMicrotask`) |
| `loop.call_later(1, f)` | `setTimeout(f, 1000)` |
| `await asyncio.sleep(0)` to yield | `await setImmediate()` from `node:timers/promises` |
| `run_in_executor` | Thread pool / worker threads |
| `PYTHONASYNCIODEBUG` slow callback warnings | `monitorEventLoopDelay` |
