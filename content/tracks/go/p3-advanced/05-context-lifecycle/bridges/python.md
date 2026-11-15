---
from: python
status: draft
concept: "Cancellation and timeouts in asyncio"
from_way: "task.cancel() raising CancelledError inside the coroutine, asyncio.timeout() blocks, contextvars for request-scoped data, and TaskGroup cancelling siblings on failure."
native_way: "Cancellation is cooperative and explicit: a context is passed to every call, code checks ctx.Done() or passes ctx on, and ctx.Err() says why it stopped."
sensei_tip: "asyncio injects CancelledError at the next await; Go never interrupts your code. If a loop never looks at ctx, it never stops: check it."
---

| Python asyncio | Go |
| --- | --- |
| `task.cancel()` | `cancel()` |
| `except asyncio.CancelledError` | `if errors.Is(err, context.Canceled)` |
| `async with asyncio.timeout(2):` | `ctx, cancel := context.WithTimeout(ctx, 2*time.Second)` |
| `contextvars.ContextVar("request_id")` | `context.WithValue` + a typed getter |
| `TaskGroup` cancelling on the first error | `errgroup.WithContext` |
| `asyncio.shield(coro)` | `context.WithoutCancel(ctx)` |
| `loop.add_signal_handler(SIGTERM, ...)` | `signal.NotifyContext` |
