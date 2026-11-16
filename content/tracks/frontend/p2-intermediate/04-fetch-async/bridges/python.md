---
from: python
status: draft
concept: "Async I/O"
from_way: "asyncio with httpx or aiohttp: async def, await, asyncio.gather and task cancellation."
native_way: "The browser's event loop is always running; async functions return Promises, Promise.all is gather, and AbortController is cancellation."
sensei_tip: "asyncio.gather is Promise.all, and raise_for_status() is the check fetch makes you write yourself."
---

| Python | In the browser |
| --- | --- |
| `await client.get(url)` | `await fetch(url)` |
| `response.raise_for_status()` | `if (!response.ok) throw ...` |
| `asyncio.gather(a, b)` | `Promise.all([a, b])` |
| `task.cancel()` | `controller.abort()` |
