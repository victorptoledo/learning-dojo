---
from: python
status: draft
concept: "Queues and asyncio coordination"
from_way: "queue.Queue or asyncio.Queue with a sentinel None at the end, asyncio.wait with FIRST_COMPLETED, asyncio.wait_for for timeouts and asyncio.Semaphore for limits."
native_way: "Typed channels with close instead of a sentinel, select instead of asyncio.wait, timers and ctx.Done() for timeouts, and a buffered channel as a semaphore."
sensei_tip: "A Go pipeline stage is a generator that runs in parallel: it reads from one channel and yields to the next, and closing its output is the StopIteration of the stage."
---

| Python | Go |
| --- | --- |
| `asyncio.Queue()` | `make(chan T)` |
| `asyncio.Queue(maxsize=10)` | `make(chan T, 10)` |
| `await q.put(x)` / `await q.get()` | `ch <- x` / `x := <-ch` |
| sentinel `None` at the end | `close(ch)` |
| `asyncio.wait(..., return_when=FIRST_COMPLETED)` | `select` |
| `asyncio.wait_for(coro, 2)` | `case <-time.After(2 * time.Second):` |
| `asyncio.Semaphore(3)` | `make(chan struct{}, 3)` |
| `q.get_nowait()` | `select` with `default` |
