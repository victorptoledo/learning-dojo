---
from: python
status: draft
concept: "Async generators and async for"
from_way: "async def with yield, async for, aclose() running finally, aiofiles or asyncio streams for I/O, asyncio.Semaphore to limit concurrency, task cancellation."
native_way: "async function* with yield, for await, return() running finally, Node.js streams and readline as async iterables, a pool of pending promises, AbortSignal."
sensei_tip: "The shapes match one to one. The big difference is cancellation: Python cancels a task by raising CancelledError at its await, while TypeScript only stops what you passed an AbortSignal to."
---

| Python | TypeScript |
| --- | --- |
| `async def gen(): yield x` | `async function* gen() { yield x; }` |
| `async for x in gen()` | `for await (const x of gen())` |
| `await agen.aclose()` | `await agen.return(undefined)` |
| `AsyncIterator[str]` | `AsyncIterable<string>` / `AsyncGenerator<string, void, undefined>` |
| `[x async for x in gen()]` | `await Array.fromAsync(gen())` |
| `asyncio.Semaphore(4)` | A pool of at most 4 pending promises |
| `task.cancel()` | `controller.abort()` |
| `async with` | `await using` |
