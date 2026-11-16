---
from: python
status: draft
concept: "asyncio tasks you can cancel"
from_way: "You await httpx calls, cancel tasks with task.cancel(), check response.raise_for_status(), and cache with functools.lru_cache."
native_way: "fetch returns a promise, AbortController cancels it, you check response.ok yourself, and a server-state library caches by key with expiry."
sensei_tip: "fetch never calls raise_for_status for you: always check response.ok. And remember that an effect re-running is like a new task: cancel the old one."
---

| Python | React |
| --- | --- |
| `await client.get(url)` | `await fetch(url)` |
| `response.raise_for_status()` | `if (!response.ok) throw ...` |
| `task.cancel()` | `controller.abort()` |
| `asyncio.gather(a, b)` | `Promise.all([a, b])` |
| `functools.lru_cache` | TanStack Query cache |
| Django view loads data before rendering | Framework loaders |
