---
from: python
status: draft
concept: "Modern asyncio"
from_way: "@asyncio.coroutine with yield from, loop = get_event_loop() and run_until_complete, ensure_future, gather without error handling, wait_for for timeouts, run_in_executor, and requests inside coroutines."
native_way: "async def and await, asyncio.run, TaskGroup for structured concurrency, asyncio.timeout, asyncio.to_thread, async clients such as httpx, and except* for task group errors."
sensei_tip: "If you see get_event_loop() in application code, it is old: asyncio.run(main()) is the only entry point you need, and TaskGroup replaces most create_task bookkeeping."
---

| Older habit | Python 3.15 |
| --- | --- |
| `@asyncio.coroutine` + `yield from` | `async def` + `await` |
| `loop = asyncio.get_event_loop(); loop.run_until_complete(main())` | `asyncio.run(main())` |
| `asyncio.ensure_future(coro)` and tracking tasks | `async with asyncio.TaskGroup() as tg: tg.create_task(coro)` |
| `await asyncio.wait_for(coro, 3)` | `async with asyncio.timeout(3): await coro` |
| `loop.run_in_executor(None, f, x)` | `await asyncio.to_thread(f, x)` |
| `requests.get` inside a coroutine | `await httpx.AsyncClient().get(...)` |
| "Python can never run threads in parallel" | true with the GIL; free-threaded builds (3.13t to 3.15t) can |
