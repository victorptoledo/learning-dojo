---
from: python
status: draft
concept: "asyncio and exceptions"
from_way: "async def and await on an asyncio event loop, asyncio.gather and TaskGroup, exceptions with raise ... from, ExceptionGroup, context managers."
native_way: "async functions and await on the event loop that is always running, Promise.all and allSettled, errors with cause, AggregateError, using declarations."
sensei_tip: "The model is the same as asyncio, but there is no loop to start and no TaskGroup: an unawaited promise keeps running. Lint for floating promises the way you would watch for un-awaited coroutines."
---

| Python | TypeScript |
| --- | --- |
| `async def load() -> User` | `async function load(): Promise<User>` |
| `await asyncio.gather(a(), b())` | `await Promise.all([a(), b()])` |
| `gather(..., return_exceptions=True)` | `Promise.allSettled` |
| `raise AppError("x") from err` | `throw new AppError("x", { cause: err })` |
| `ExceptionGroup` | `AggregateError` |
| `async with` / `with` | `await using` / `using` |
| `asyncio.timeout(5)` | `AbortSignal.timeout(5000)` |
