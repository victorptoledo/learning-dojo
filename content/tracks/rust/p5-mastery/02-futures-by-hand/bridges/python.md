---
from: python
status: draft
concept: "The event loop underneath"
from_way: "`asyncio` coroutines are driven by an event loop that resumes them when an awaited `Future` completes."
native_way: "Futures are polled by an executor and woken through a `Waker`; there is no built-in loop."
sensei_tip: "Python ships one event loop; Rust ships only the `Future` trait, so `block_on` is twenty lines you can write yourself."
---

| Python | Rust |
| --- | --- |
| `asyncio.run(coro)` | `block_on(future)` |
| `asyncio.gather(a, b)` | `join_all` / `tokio::join!` |
| `asyncio.wait_for(c, 1)` | `timeout(Duration::from_secs(1), f)` |
| `future.set_result` wakes the loop | `waker.wake()` wakes the executor |
