---
from: python
status: draft
concept: "Coroutines and futures"
from_way: "async def and await on asyncio, asyncio.gather and TaskGroup, wait_for for timeouts, and task.cancel() raising CancelledError inside the coroutine."
native_way: "async functions and await on the always-running loop, Promise.all and allSettled, AbortSignal for timeouts and cancellation."
sensei_tip: "A coroutine does nothing until awaited; a promise starts running as soon as it is created. In Node.js, calling an async function without await still runs it, and its errors become unhandled rejections."
---

| Python | Node.js |
| --- | --- |
| `await asyncio.gather(a(), b())` | `await Promise.all([a(), b()])` |
| `gather(..., return_exceptions=True)` | `Promise.allSettled` |
| `asyncio.wait_for(x, 2)` | `AbortSignal.timeout(2000)` |
| `task.cancel()` | `controller.abort()` |
| `async for` / async generators | `for await` / `async function*` |
| `loop.run_in_executor` | promisified APIs on the thread pool |
