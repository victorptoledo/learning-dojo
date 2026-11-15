# asyncio, Threads, Processes, the GIL and Free-Threading

Python offers three ways to do several things at once, and the right one depends on one
question: **is the work waiting (I/O-bound) or computing (CPU-bound)?** This lesson explains
the global interpreter lock that shapes the answer, what the new free-threaded build changes,
and how to write correct `asyncio` code.

## I/O-bound and CPU-bound

| Work | Spends its time | Examples | Best tool on standard CPython |
| --- | --- | --- | --- |
| **I/O-bound** | Waiting for the network, disk or a database | HTTP calls, queries, file transfers | `asyncio` (many connections) or threads |
| **CPU-bound** | Executing Python code | Parsing, image work, simulations, pure-Python maths | Processes, or native code that releases the GIL (NumPy, Rust via PyO3) |

## The GIL

CPython's **global interpreter lock** lets only **one thread execute Python bytecode at a
time** in a process. It exists because CPython's memory management (reference counting) is
not thread-safe without it, and a single lock made the interpreter and C extensions simple and
fast for single-threaded code.

Two consequences:

- Threads still help **I/O**: a thread releases the GIL while it waits on a socket or a file,
  so other threads run. Many C libraries (zlib, hashlib, NumPy) release it during heavy work
  too.
- Threads do **not** speed up pure-Python CPU work: ten threads crunching numbers take as long
  as one, or longer.

The GIL does **not** make your code thread-safe. `counter += 1` is several bytecode operations,
and a thread switch can happen between them:

```python
lock = threading.Lock()
with lock:              # protect shared state, GIL or not
    counter += 1
```

### Free-threaded Python

PEP 703 made the GIL optional. **Python 3.13** shipped an *experimental* free-threaded build
in which threads run Python code truly in parallel. **Python 3.14** made that build officially
supported (PEP 779), with a much smaller single-thread cost. **Python 3.15** keeps it a
separate build (`python3.15t`, installable with `uv python install 3.15t`), **not the default**,
and adds a stable ABI for free-threaded extensions (PEP 803), so libraries can ship one wheel
that works across versions; the official macOS installer now installs it alongside the
standard build. `sys._is_gil_enabled()` tells you which mode you are in.

What that means in practice: pure-Python CPU work can scale across cores with ordinary threads,
**if** every C extension you use declares support for it (otherwise the GIL is turned back on
when it is imported), and your code protects shared state with locks, because the GIL's
accidental protection is gone. For most services today, the advice below still holds.

## Threads and processes: concurrent.futures

```python
from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor

with ThreadPoolExecutor(max_workers=20) as pool:          # I/O with blocking libraries
    pages = list(pool.map(download, urls))

def main() -> None:
    with ProcessPoolExecutor() as pool:                   # CPU work, one process per core
        thumbnails = list(pool.map(resize, images, chunksize=16))

if __name__ == "__main__":     # required: worker processes re-import this module
    main()
```

Processes sidestep the GIL completely, at a cost: arguments and results are **pickled** and
copied between processes, and each process has its own memory. Send small inputs, do a lot of
work per item and use `chunksize` for many small items.

## asyncio

`asyncio` runs thousands of I/O operations on **one thread**, with an **event loop** that
switches between coroutines whenever one of them waits.

```python
import asyncio
import httpx

async def fetch(client: httpx.AsyncClient, url: str) -> int:
    response = await client.get(url)       # await: "pause me here, run something else"
    return response.status_code

async def main() -> None:
    async with httpx.AsyncClient() as client:
        status = await fetch(client, "https://example.org")

asyncio.run(main())                         # starts the loop, runs main, closes the loop
```

| Concept | Meaning |
| --- | --- |
| `async def` | Defines a coroutine function; calling it creates a coroutine object, and nothing runs yet |
| `await x` | Suspends this coroutine until `x` (a coroutine, task or future) is done |
| Task | A coroutine scheduled on the loop to run concurrently: `asyncio.create_task(coro)` or `tg.create_task(coro)` |
| `async with` / `async for` | Context managers and iterators whose enter, exit and next can await |

It is **cooperative**: a coroutine only gives up control at an `await`. One blocking call
(`time.sleep`, `requests.get`, a heavy loop) freezes **every** coroutine on the loop. Use async
libraries (httpx, asyncpg, aiofiles) or push blocking work to a thread:
`await asyncio.to_thread(blocking_function, arg)`.

### Running things concurrently: TaskGroup

```python
async def fetch_all(urls: list[str]) -> list[int]:
    async with httpx.AsyncClient() as client:
        async with asyncio.TaskGroup() as tg:                 # 3.11+
            tasks = [tg.create_task(fetch(client, url)) for url in urls]
    return [task.result() for task in tasks]                 # all done when the block exits
```

A `TaskGroup` is **structured concurrency**: the block waits for every task; if one fails, the
others are **cancelled** and the errors are raised together as an `ExceptionGroup` (handle them
with `except*`). `asyncio.gather(*coros)` is the older API; with `return_exceptions=True` it
returns errors as values instead of cancelling, which is useful when every result matters on
its own. Since 3.15, `tg.cancel()` ends a group early **without** an error, for example once
the first useful answer has arrived.

### Timeouts, limits and queues

```python
limit = asyncio.Semaphore(50)                  # at most 50 requests in flight

async def polite_fetch(client, url):
    async with limit:
        async with asyncio.timeout(3):          # 3.11+: raises TimeoutError if exceeded
            return await client.get(url)
```

`asyncio.Queue` connects producers and consumers with back pressure: a fixed number of worker
tasks `await queue.get()` in a loop, which bounds concurrency as a semaphore does and also
keeps memory flat.

### Cancellation

Cancelling a task (by a timeout, a `TaskGroup` or `task.cancel()`) raises `CancelledError`
**inside** it at its current `await`. Clean up with `try`/`finally` or `async with`, and **let
`CancelledError` propagate**: catching it and carrying on breaks timeouts and task groups.
(`except Exception` does not catch it, since it derives from `BaseException`.)

## Choosing

| Situation | Choose |
| --- | --- |
| Many network calls, async libraries available | `asyncio` with `TaskGroup`, `Semaphore`, `timeout` |
| A few blocking calls (a sync SDK) | `ThreadPoolExecutor`, or `asyncio.to_thread` inside async code |
| CPU-heavy pure Python | `ProcessPoolExecutor`; or the free-threaded build with threads, if your libraries support it |
| Heavy numeric work | NumPy or a native extension, which releases the GIL |
| Isolated parallel workers in one process | Subinterpreters (`concurrent.interpreters`, new in 3.14) |

## Summary

- Decide first: waiting (I/O) or computing (CPU)?
- The GIL serialises bytecode, not I/O; it never makes shared state safe. Free-threading is
  experimental in 3.13, supported since 3.14, still a separate, optional build in 3.15.
- Threads for blocking I/O, processes for CPU work, both through `concurrent.futures`.
- asyncio: one thread, cooperative; never block the loop; `TaskGroup`, `timeout`, `Semaphore`,
  `Queue`; let `CancelledError` propagate.

## Further reading

- `asyncio`, "Coroutines and Tasks": <https://docs.python.org/3/library/asyncio-task.html>
- `asyncio`, "Developing with asyncio" (blocking code, debug mode): <https://docs.python.org/3/library/asyncio-dev.html>
- `concurrent.futures`: <https://docs.python.org/3/library/concurrent.futures.html>
- Python support for free threading (HOWTO): <https://docs.python.org/3/howto/free-threading-python.html>
- PEP 703, making the GIL optional: <https://peps.python.org/pep-0703/>
- PEP 779, criteria for supported status of free-threaded Python: <https://peps.python.org/pep-0779/>
- roadmap.sh, Python topic "GIL": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/gil@bS7WeVKm2kEElu3sBKcIC.md>
